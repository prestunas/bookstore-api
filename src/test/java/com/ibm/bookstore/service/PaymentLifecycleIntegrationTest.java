package com.ibm.bookstore.service;

import com.ibm.bookstore.dto.AddCartItemRequest;
import com.ibm.bookstore.dto.CheckoutRequest;
import com.ibm.bookstore.dto.OrderResponse;
import com.ibm.bookstore.dto.PaymentRequest;
import com.ibm.bookstore.dto.PaymentResponse;
import com.ibm.bookstore.entity.Book;
import com.ibm.bookstore.entity.Order;
import com.ibm.bookstore.entity.OrderStatus;
import com.ibm.bookstore.entity.Payment;
import com.ibm.bookstore.entity.PaymentStatus;
import com.ibm.bookstore.entity.User;
import com.ibm.bookstore.entity.UserAddress;
import com.ibm.bookstore.exception.PaymentProcessingException;
import com.ibm.bookstore.repository.BookRepository;
import com.ibm.bookstore.repository.CartRepository;
import com.ibm.bookstore.repository.OrderRepository;
import com.ibm.bookstore.repository.PaymentRepository;
import com.ibm.bookstore.repository.UserAddressRepository;
import com.ibm.bookstore.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "bookstore.security.jwt.secret=test-secret-key-minimum-32-bytes!!",
        "bookstore.security.jwt.expiration-ms=86400000"
})
class PaymentLifecycleIntegrationTest {

    @Autowired
    CartService cartService;

    @Autowired
    OrderService orderService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserAddressRepository userAddressRepository;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    CartRepository cartRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Test
    @DisplayName("Full Purchase Lifecycle: Cart -> Checkout -> Payment -> Reward Points -> Stock Decrement -> Cart Clear")
    void fullPurchaseLifecycle_Success() {
        String username = "customer_demo";
        User user = userRepository.findByUsername(username).orElseThrow();
        UserAddress address = userAddressRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).getFirst();
        List<Book> books = bookRepository.findAll();
        Book book = books.getFirst();

        int initialStock = book.getStockQuantity();
        int initialUserPoints = user.getRewardPoints();

        // 1. Add item to cart
        cartService.clearCart(username);
        cartService.addItem(username, new AddCartItemRequest(book.getId(), 2));

        // 2. Checkout with 100 gift points redeemed ($1.00 discount)
        CheckoutRequest checkoutRequest = new CheckoutRequest(address.getId(), 100);
        OrderResponse orderResponse = orderService.checkout(username, checkoutRequest);

        assertThat(orderResponse.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(orderResponse.pointsRedeemed()).isEqualTo(100);
        assertThat(orderResponse.pointsEarned()).isEqualTo(0);

        // At checkout, points are reserved on order, user balance still intact until payment
        User userAfterCheckout = userRepository.findByUsername(username).orElseThrow();
        assertThat(userAfterCheckout.getRewardPoints()).isEqualTo(initialUserPoints);

        // Stock not decremented yet
        Book bookAfterCheckout = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(bookAfterCheckout.getStockQuantity()).isEqualTo(initialStock);

        // 3. Process Payment
        PaymentRequest paymentRequest = new PaymentRequest("CREDIT_CARD", "SUCCESS", "Live PostgreSQL flow test");
        PaymentResponse paymentResponse = paymentService.processPayment(username, orderResponse.id(), paymentRequest);

        assertThat(paymentResponse.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(paymentResponse.paymentReference()).startsWith("PAY-");

        // 4. Verify post-payment state
        Order paidOrder = orderRepository.findById(orderResponse.id()).orElseThrow();
        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        int expectedEarned = paidOrder.getSubtotal().intValue();
        assertThat(paidOrder.getPointsEarned()).isEqualTo(expectedEarned);

        // User points: initial - 100 redeemed + expectedEarned
        User userAfterPayment = userRepository.findByUsername(username).orElseThrow();
        assertThat(userAfterPayment.getRewardPoints()).isEqualTo(initialUserPoints - 100 + expectedEarned);

        // Stock decremented by 2
        Book bookAfterPayment = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(bookAfterPayment.getStockQuantity()).isEqualTo(initialStock - 2);

        // Cart is cleared
        assertThat(cartService.getCart(username).items()).isEmpty();

        // 5. Verify repeated payment is idempotent
        PaymentResponse repeatedResponse = paymentService.processPayment(username, orderResponse.id(), paymentRequest);
        assertThat(repeatedResponse.paymentReference()).isEqualTo(paymentResponse.paymentReference());
        assertThat(repeatedResponse.status()).isEqualTo(PaymentStatus.SUCCESS);

        // 6. Cancel PAID order within window -> stock restored, redeemed points refunded, earned points reversed
        OrderResponse cancelledOrder = orderService.cancelOrder(username, orderResponse.id());
        assertThat(cancelledOrder.status()).isEqualTo(OrderStatus.CANCELLED);

        Book bookAfterCancel = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(bookAfterCancel.getStockQuantity()).isEqualTo(initialStock);

        User userAfterCancel = userRepository.findByUsername(username).orElseThrow();
        assertThat(userAfterCancel.getRewardPoints()).isEqualTo(initialUserPoints);
    }

    @Test
    @DisplayName("Failed Payment Lifecycle: Failed payment record is persisted in PostgreSQL despite transaction exception")
    void failedPaymentLifecycle_RecordPersistedInPostgres() {
        String username = "customer_demo";
        User user = userRepository.findByUsername(username).orElseThrow();
        UserAddress address = userAddressRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).getFirst();
        List<Book> books = bookRepository.findAll();
        Book book = books.getFirst();

        int initialStock = book.getStockQuantity();
        int initialUserPoints = user.getRewardPoints();

        cartService.clearCart(username);
        cartService.addItem(username, new AddCartItemRequest(book.getId(), 1));

        CheckoutRequest checkoutRequest = new CheckoutRequest(address.getId(), 0);
        OrderResponse orderResponse = orderService.checkout(username, checkoutRequest);

        PaymentRequest failedRequest = new PaymentRequest("CREDIT_CARD", "INSUFFICIENT_FUNDS", "Declined simulation");

        assertThatThrownBy(() -> paymentService.processPayment(username, orderResponse.id(), failedRequest))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Insufficient funds");

        // Verify order remains PENDING_PAYMENT
        Order orderAfterFailure = orderRepository.findById(orderResponse.id()).orElseThrow();
        assertThat(orderAfterFailure.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        // Verify failed payment record was persisted in database
        List<Payment> payments = paymentRepository.findByOrderId(orderResponse.id());
        assertThat(payments).hasSize(1);
        Payment failedPayment = payments.getFirst();
        assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failedPayment.getFailureReason()).contains("Insufficient funds");

        // Verify stock & points unchanged
        Book bookAfterFailure = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(bookAfterFailure.getStockQuantity()).isEqualTo(initialStock);
        User userAfterFailure = userRepository.findByUsername(username).orElseThrow();
        assertThat(userAfterFailure.getRewardPoints()).isEqualTo(initialUserPoints);

        // Clean up: cancel order
        orderService.cancelOrder(username, orderResponse.id());
    }
}

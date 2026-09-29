package com.ibm.bookstore.service;

import com.ibm.bookstore.dto.PaymentRequest;
import com.ibm.bookstore.dto.PaymentResponse;
import com.ibm.bookstore.entity.Author;
import com.ibm.bookstore.entity.Book;
import com.ibm.bookstore.entity.Cart;
import com.ibm.bookstore.entity.Category;
import com.ibm.bookstore.entity.Order;
import com.ibm.bookstore.entity.OrderItem;
import com.ibm.bookstore.entity.OrderStatus;
import com.ibm.bookstore.entity.Payment;
import com.ibm.bookstore.entity.PaymentStatus;
import com.ibm.bookstore.entity.Publisher;
import com.ibm.bookstore.entity.User;
import com.ibm.bookstore.exception.BusinessRuleException;
import com.ibm.bookstore.exception.InsufficientStockException;
import com.ibm.bookstore.exception.PaymentProcessingException;
import com.ibm.bookstore.exception.ResourceNotFoundException;
import com.ibm.bookstore.repository.BookRepository;
import com.ibm.bookstore.repository.CartRepository;
import com.ibm.bookstore.repository.OrderRepository;
import com.ibm.bookstore.repository.PaymentRepository;
import com.ibm.bookstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    PaymentRecordService paymentRecordService;

    @Mock
    UserRepository userRepository;

    @Mock
    BookRepository bookRepository;

    @Mock
    CartRepository cartRepository;

    PaymentService paymentService;

    Instant fixedInstant;
    User testUser;
    Book testBook1;
    Book testBook2;
    Order testOrder;
    Cart testCart;

    @BeforeEach
    void setUp() {
        fixedInstant = Instant.parse("2026-05-10T12:00:00Z");
        Clock clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

        paymentService = new PaymentService(
                orderRepository,
                paymentRepository,
                paymentRecordService,
                userRepository,
                bookRepository,
                cartRepository,
                clock
        );

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashed")
                .fullName("Test User")
                .role("ROLE_CUSTOMER")
                .rewardPoints(500)
                .active(true)
                .build();

        Category category = Category.builder().id(UUID.randomUUID()).name("Software").slug("software").build();
        Author author = Author.builder().id(UUID.randomUUID()).name("Martin Fowler").build();
        Publisher publisher = Publisher.builder().id(UUID.randomUUID()).name("Addison-Wesley").build();

        testBook1 = Book.builder()
                .id(UUID.randomUUID())
                .title("Clean Code")
                .isbn("978-0132350884")
                .price(new BigDecimal("30.00"))
                .stockQuantity(10)
                .active(true)
                .category(category)
                .author(author)
                .publisher(publisher)
                .build();

        testBook2 = Book.builder()
                .id(UUID.randomUUID())
                .title("Refactoring")
                .isbn("978-0201485677")
                .price(new BigDecimal("25.00"))
                .stockQuantity(5)
                .active(true)
                .category(category)
                .author(author)
                .publisher(publisher)
                .build();

        testOrder = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-12345678-ABCD")
                .user(testUser)
                .status(OrderStatus.PENDING_PAYMENT)
                .subtotal(new BigDecimal("85.00"))
                .discountAmount(new BigDecimal("2.00"))
                .pointsRedeemed(200)
                .pointsEarned(0)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("83.00"))
                .orderedAt(fixedInstant)
                .recipientName("Test User")
                .phone("+1-555-0100")
                .street("123 Main St")
                .city("City")
                .state("State")
                .postalCode("12345")
                .country("USA")
                .items(new ArrayList<>())
                .build();

        OrderItem item1 = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(testOrder)
                .book(testBook1)
                .bookTitle(testBook1.getTitle())
                .unitPrice(testBook1.getPrice())
                .quantity(2)
                .subtotal(new BigDecimal("60.00"))
                .build();

        OrderItem item2 = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(testOrder)
                .book(testBook2)
                .bookTitle(testBook2.getTitle())
                .unitPrice(testBook2.getPrice())
                .quantity(1)
                .subtotal(new BigDecimal("25.00"))
                .build();

        testOrder.getItems().add(item1);
        testOrder.getItems().add(item2);

        testCart = Cart.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .items(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("processPayment - SUCCESS outcome updates order to PAID, decrements stock, credits points, and clears cart")
    void processPayment_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "SUCCESS", "Test payment note");
        PaymentResponse response = paymentService.processPayment("testuser", testOrder.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(testOrder.getId());
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("83.00"));
        assertThat(response.paymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(response.failureReason()).isNull();

        // Stock checks: book1 (10 -> 8), book2 (5 -> 4)
        assertThat(testBook1.getStockQuantity()).isEqualTo(8);
        assertThat(testBook2.getStockQuantity()).isEqualTo(4);
        verify(bookRepository).save(testBook1);
        verify(bookRepository).save(testBook2);

        // Order status check
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(testOrder.getPointsEarned()).isEqualTo(85);
        verify(orderRepository).save(testOrder);

        // User points check: initial 500 - 200 redeemed + 85 earned = 385
        assertThat(testUser.getRewardPoints()).isEqualTo(385);
        verify(userRepository).save(testUser);

        // Cart should be cleared
        assertThat(testCart.getItems()).isEmpty();
        verify(cartRepository).save(testCart);
    }

    @Test
    @DisplayName("processPayment - repeated payment for already PAID order returns existing successful payment idempotently")
    void processPayment_AlreadyPaid_ReturnsExistingPayment() {
        testOrder.setStatus(OrderStatus.PAID);
        Payment existingPayment = Payment.builder()
                .id(UUID.randomUUID())
                .order(testOrder)
                .paymentReference("PAY-EXISTING-123")
                .paymentMethod("CREDIT_CARD")
                .amount(testOrder.getTotalAmount())
                .status(PaymentStatus.SUCCESS)
                .failureReason(null)
                .paidAt(fixedInstant)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(paymentRepository.findByOrderId(testOrder.getId())).thenReturn(List.of(existingPayment));

        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "SUCCESS", "Repeated call");
        PaymentResponse response = paymentService.processPayment("testuser", testOrder.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.paymentReference()).isEqualTo("PAY-EXISTING-123");
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("processPayment - INSUFFICIENT_FUNDS outcome records failed payment and throws PaymentProcessingException")
    void processPayment_InsufficientFunds_ThrowsPaymentProcessingException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "INSUFFICIENT_FUNDS", "Failed sim");

        assertThatThrownBy(() -> paymentService.processPayment("testuser", testOrder.getId(), request))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Insufficient funds");

        verify(paymentRecordService).recordFailedPayment(
                eq(testOrder),
                any(String.class),
                eq("CREDIT_CARD"),
                eq(testOrder.getTotalAmount()),
                eq("Payment failed: Insufficient funds in account."),
                eq("Failed sim"),
                eq(fixedInstant)
        );
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(orderRepository, never()).save(testOrder);
        verify(bookRepository, never()).save(any(Book.class));
        verify(userRepository, never()).save(testUser);
    }

    @Test
    @DisplayName("processPayment - GATEWAY_TIMEOUT outcome records failed payment and throws PaymentProcessingException")
    void processPayment_GatewayTimeout_ThrowsPaymentProcessingException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

        PaymentRequest request = new PaymentRequest("DEBIT_CARD", "GATEWAY_TIMEOUT", null);

        assertThatThrownBy(() -> paymentService.processPayment("testuser", testOrder.getId(), request))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("timed out");
    }

    @Test
    @DisplayName("processPayment - CARD_EXPIRED outcome records failed payment and throws PaymentProcessingException")
    void processPayment_CardExpired_ThrowsPaymentProcessingException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "CARD_EXPIRED", null);

        assertThatThrownBy(() -> paymentService.processPayment("testuser", testOrder.getId(), request))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Card has expired");
    }

    @Test
    @DisplayName("processPayment - order not belonging to current user throws ResourceNotFoundException")
    void processPayment_OrderBelongsToOtherUser_ThrowsNotFound() {
        User otherUser = User.builder().id(UUID.randomUUID()).username("other").build();
        testOrder.setUser(otherUser);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "SUCCESS", null);

        assertThatThrownBy(() -> paymentService.processPayment("testuser", testOrder.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");
    }

    @Test
    @DisplayName("processPayment - insufficient stock when paying throws InsufficientStockException")
    void processPayment_InsufficientStock_ThrowsException() {
        testBook1.setStockQuantity(1); // Requested 2

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "SUCCESS", null);

        assertThatThrownBy(() -> paymentService.processPayment("testuser", testOrder.getId(), request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("processPayment - CANCELLED order cannot be paid and throws BusinessRuleException")
    void processPayment_CancelledOrder_ThrowsException() {
        testOrder.setStatus(OrderStatus.CANCELLED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "SUCCESS", null);

        assertThatThrownBy(() -> paymentService.processPayment("testuser", testOrder.getId(), request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Order cannot be paid in current status: CANCELLED");
    }
}

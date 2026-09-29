package com.ibm.bookstore.service;

import com.ibm.bookstore.dto.PaymentRequest;
import com.ibm.bookstore.dto.PaymentResponse;
import com.ibm.bookstore.entity.Book;
import com.ibm.bookstore.entity.Order;
import com.ibm.bookstore.entity.OrderItem;
import com.ibm.bookstore.entity.OrderStatus;
import com.ibm.bookstore.entity.Payment;
import com.ibm.bookstore.entity.PaymentStatus;
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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentService {

    OrderRepository orderRepository;
    PaymentRepository paymentRepository;
    PaymentRecordService paymentRecordService;
    UserRepository userRepository;
    BookRepository bookRepository;
    CartRepository cartRepository;
    Clock clock;

    @Transactional
    public PaymentResponse processPayment(String username, UUID orderId, PaymentRequest request) {
        User user = getUser(username);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order", orderId);
        }

        if (order.getStatus() == OrderStatus.PAID) {
            List<Payment> existingPayments = paymentRepository.findByOrderId(orderId);
            Payment successfulPayment = existingPayments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                    .findFirst()
                    .orElse(null);

            if (successfulPayment != null) {
                return toPaymentResponse(successfulPayment);
            }
            throw new BusinessRuleException("Order is already paid.");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("Order cannot be paid in current status: " + order.getStatus());
        }

        Instant now = clock.instant();
        String paymentReference = generatePaymentReference(now);
        String simulationOutcome = request.simulationOutcome() != null
                ? request.simulationOutcome().trim().toUpperCase()
                : "SUCCESS";

        if (!"SUCCESS".equals(simulationOutcome)) {
            String failureReason = switch (simulationOutcome) {
                case "INSUFFICIENT_FUNDS" -> "Payment failed: Insufficient funds in account.";
                case "GATEWAY_TIMEOUT" -> "Payment failed: Payment gateway timed out.";
                case "CARD_EXPIRED" -> "Payment failed: Card has expired.";
                default -> "Payment failed: " + simulationOutcome;
            };

            paymentRecordService.recordFailedPayment(
                    order,
                    paymentReference,
                    request.paymentMethod(),
                    order.getTotalAmount(),
                    failureReason,
                    request.paymentNotes(),
                    now
            );
            throw new PaymentProcessingException(failureReason);
        }

        // Validate stock availability for all items before decrementing
        for (OrderItem item : order.getItems()) {
            Book book = item.getBook();
            if (book.getStockQuantity() < item.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for book '" + book.getTitle() + "'. Available: "
                                + book.getStockQuantity() + ", requested: " + item.getQuantity()
                );
            }
        }

        // Decrement stock
        for (OrderItem item : order.getItems()) {
            Book book = item.getBook();
            book.setStockQuantity(book.getStockQuantity() - item.getQuantity());
            bookRepository.save(book);
        }

        // Deduct redeemed points and credit earned reward points (1 point per $1.00 spent on subtotal, floored)
        int pointsRedeemed = order.getPointsRedeemed() != null ? order.getPointsRedeemed() : 0;
        if (pointsRedeemed > user.getRewardPoints()) {
            throw new BusinessRuleException("Insufficient reward points to complete payment.");
        }
        int pointsToCredit = order.getSubtotal().setScale(0, RoundingMode.FLOOR).intValue();
        order.setPointsEarned(pointsToCredit);
        user.setRewardPoints(user.getRewardPoints() - pointsRedeemed + pointsToCredit);
        userRepository.save(user);

        // Transition order status to PAID
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        // Clear user's active shopping cart
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });

        // Save successful payment record
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .order(order)
                .paymentReference(paymentReference)
                .paymentMethod(request.paymentMethod())
                .amount(order.getTotalAmount())
                .status(PaymentStatus.SUCCESS)
                .failureReason(null)
                .paymentNotes(request.paymentNotes())
                .paidAt(now)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        return toPaymentResponse(savedPayment);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }

    private String generatePaymentReference(Instant now) {
        String hexSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "PAY-" + now.toEpochMilli() + "-" + hexSuffix;
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getPaymentReference(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getPaidAt()
        );
    }
}

package com.ibm.bookstore.service;

import com.ibm.bookstore.entity.Order;
import com.ibm.bookstore.entity.Payment;
import com.ibm.bookstore.entity.PaymentStatus;
import com.ibm.bookstore.repository.PaymentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentRecordService {

    PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment recordFailedPayment(
            Order order,
            String paymentReference,
            String paymentMethod,
            BigDecimal amount,
            String failureReason,
            String paymentNotes,
            Instant paidAt
    ) {
        Payment failedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .order(order)
                .paymentReference(paymentReference)
                .paymentMethod(paymentMethod)
                .amount(amount)
                .status(PaymentStatus.FAILED)
                .failureReason(failureReason)
                .paymentNotes(paymentNotes)
                .paidAt(paidAt)
                .build();

        return paymentRepository.save(failedPayment);
    }
}

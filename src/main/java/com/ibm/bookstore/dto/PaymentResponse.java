package com.ibm.bookstore.dto;

import com.ibm.bookstore.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String paymentReference,
        String paymentMethod,
        BigDecimal amount,
        PaymentStatus status,
        String failureReason,
        Instant paidAt
) {
}

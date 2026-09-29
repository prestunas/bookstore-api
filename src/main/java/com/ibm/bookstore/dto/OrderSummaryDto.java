package com.ibm.bookstore.dto;

import com.ibm.bookstore.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryDto(
        UUID id,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant orderedAt,
        Integer totalItems
) {
}

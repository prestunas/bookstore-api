package com.ibm.bookstore.dto;

import com.ibm.bookstore.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        OrderStatus status,
        List<OrderItemResponse> items,
        AddressDto shippingAddress,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        Integer pointsRedeemed,
        Integer pointsEarned,
        BigDecimal taxAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        Instant orderedAt,
        Boolean canBeCancelled
) {
}

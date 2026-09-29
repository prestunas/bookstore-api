package com.ibm.bookstore.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        List<CartItemResponse> items,
        Integer totalQuantity,
        BigDecimal totalAmount
) {
}

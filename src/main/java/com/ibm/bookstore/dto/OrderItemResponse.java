package com.ibm.bookstore.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID bookId,
        String bookTitle,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) {
}

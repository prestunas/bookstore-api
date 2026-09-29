package com.ibm.bookstore.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID bookId,
        String bookTitle,
        String bookIsbn,
        String coverImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        Integer expectedDeliveryDays
) {
}

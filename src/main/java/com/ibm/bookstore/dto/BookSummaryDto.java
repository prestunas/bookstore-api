package com.ibm.bookstore.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BookSummaryDto(
        UUID id,
        String title,
        String isbn,
        BigDecimal price,
        Integer stockQuantity,
        String coverImageUrl,
        String authorName,
        String categoryName,
        Integer expectedDeliveryDays
) {
}

package com.ibm.bookstore.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BookDetailDto(
        UUID id,
        String title,
        String isbn,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        String coverImageUrl,
        Integer expectedDeliveryDays,
        AuthorDto author,
        CategoryDto category,
        PublisherDto publisher
) {
}

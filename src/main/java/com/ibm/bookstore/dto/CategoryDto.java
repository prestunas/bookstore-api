package com.ibm.bookstore.dto;

import java.util.UUID;

public record CategoryDto(
        UUID id,
        String name,
        String slug,
        String description
) {
}

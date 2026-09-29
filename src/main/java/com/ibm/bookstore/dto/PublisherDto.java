package com.ibm.bookstore.dto;

import java.util.UUID;

public record PublisherDto(
        UUID id,
        String name,
        String website
) {
}

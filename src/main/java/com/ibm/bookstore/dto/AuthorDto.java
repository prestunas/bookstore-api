package com.ibm.bookstore.dto;

import java.util.UUID;

public record AuthorDto(
        UUID id,
        String name,
        String bio
) {
}

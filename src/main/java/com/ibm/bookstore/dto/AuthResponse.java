package com.ibm.bookstore.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        UUID userId,
        String username,
        String email,
        String role
) {
    public AuthResponse(String token, UUID userId, String username, String email, String role) {
        this(token, "Bearer", userId, username, email, role);
    }
}

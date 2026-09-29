package com.ibm.bookstore.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String role,
        Integer rewardPoints
) {
}

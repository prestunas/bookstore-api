package com.ibm.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
        @NotBlank(message = "Recipient name is required")
        @Size(max = 100, message = "Recipient name must not exceed 100 characters")
        String recipientName,

        @NotBlank(message = "Phone number is required")
        @Size(max = 30, message = "Phone number must not exceed 30 characters")
        String phone,

        @NotBlank(message = "Street address is required")
        String street,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State must not exceed 100 characters")
        String state,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country,

        Boolean isDefault
) {
}

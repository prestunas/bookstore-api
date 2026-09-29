package com.ibm.bookstore.dto;

import java.util.UUID;

public record AddressDto(
        UUID id,
        String recipientName,
        String phone,
        String street,
        String city,
        String state,
        String postalCode,
        String country,
        Boolean isDefault
) {
}

package com.ibm.bookstore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CheckoutRequest(
        @NotNull(message = "Shipping address ID is required")
        UUID addressId,

        @Min(value = 0, message = "Gift points to redeem cannot be negative")
        Integer giftPointsToRedeem
) {
}

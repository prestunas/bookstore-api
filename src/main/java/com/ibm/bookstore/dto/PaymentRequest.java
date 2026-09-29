package com.ibm.bookstore.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(
        @NotBlank(message = "Payment method is required")
        String paymentMethod,

        @NotBlank(message = "Simulation outcome is required")
        String simulationOutcome,

        String paymentNotes
) {
}

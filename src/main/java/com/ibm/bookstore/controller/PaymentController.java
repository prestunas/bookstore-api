package com.ibm.bookstore.controller;

import com.ibm.bookstore.dto.PaymentRequest;
import com.ibm.bookstore.dto.PaymentResponse;
import com.ibm.bookstore.service.PaymentService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {

    PaymentService paymentService;

    @PostMapping
    public PaymentResponse processPayment(
            @AuthenticationPrincipal String username,
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentRequest request
    ) {
        return paymentService.processPayment(username, orderId, request);
    }
}

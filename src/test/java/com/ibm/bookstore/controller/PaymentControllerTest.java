package com.ibm.bookstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.bookstore.config.JwtProperties;
import com.ibm.bookstore.config.SecurityConfig;
import com.ibm.bookstore.dto.PaymentRequest;
import com.ibm.bookstore.dto.PaymentResponse;
import com.ibm.bookstore.entity.PaymentStatus;
import com.ibm.bookstore.exception.BusinessRuleException;
import com.ibm.bookstore.exception.GlobalExceptionHandler;
import com.ibm.bookstore.exception.InsufficientStockException;
import com.ibm.bookstore.exception.PaymentProcessingException;
import com.ibm.bookstore.exception.ResourceNotFoundException;
import com.ibm.bookstore.security.JwtAuthenticationFilter;
import com.ibm.bookstore.security.JwtUtils;
import com.ibm.bookstore.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtils.class, JwtProperties.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "bookstore.security.jwt.secret=test-secret-key-minimum-32-bytes!!",
        "bookstore.security.jwt.expiration-ms=86400000"
})
class PaymentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtProperties jwtProperties;

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @MockitoBean
    PaymentService paymentService;

    String token;

    @BeforeEach
    void setUp() {
        JwtUtils jwtUtils = new JwtUtils(jwtProperties);
        token = jwtUtils.generateToken(UUID.randomUUID(), "johndoe", "ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/payments - should return 200 on successful payment")
    void processPayment_Success() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        PaymentResponse response = new PaymentResponse(
                paymentId,
                orderId,
                "PAY-20260510-ABCD",
                "CREDIT_CARD",
                new BigDecimal("83.00"),
                PaymentStatus.SUCCESS,
                null,
                Instant.parse("2026-05-10T12:00:00Z")
        );

        when(paymentService.processPayment(eq("johndoe"), eq(orderId), any(PaymentRequest.class)))
                .thenReturn(response);

        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "SUCCESS", "Optional note");

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.paymentReference").value("PAY-20260510-ABCD"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(83.00));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/payments - should return 400 ProblemDetail on payment failure")
    void processPayment_FailedSimulation_ReturnsBadRequest() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(paymentService.processPayment(eq("johndoe"), eq(orderId), any(PaymentRequest.class)))
                .thenThrow(new PaymentProcessingException("Payment failed: Insufficient funds in account."));

        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "INSUFFICIENT_FUNDS", null);

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Payment failed: Insufficient funds in account."));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/payments - should return 400 ProblemDetail on insufficient stock")
    void processPayment_InsufficientStock_ReturnsBadRequest() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(paymentService.processPayment(eq("johndoe"), eq(orderId), any(PaymentRequest.class)))
                .thenThrow(new InsufficientStockException("Insufficient stock for book 'Clean Code'. Available: 1, requested: 2"));

        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "SUCCESS", null);

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Insufficient stock for book 'Clean Code'. Available: 1, requested: 2"));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/payments - should return 404 when order is not found or foreign")
    void processPayment_OrderNotFound_ReturnsNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(paymentService.processPayment(eq("johndoe"), eq(orderId), any(PaymentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Order", orderId));

        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "SUCCESS", null);

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Order not found: " + orderId));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/payments - should return 400 when order cannot be paid in current status")
    void processPayment_InvalidOrderStatus_ReturnsBadRequest() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(paymentService.processPayment(eq("johndoe"), eq(orderId), any(PaymentRequest.class)))
                .thenThrow(new BusinessRuleException("Order cannot be paid in current status: CANCELLED"));

        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "SUCCESS", null);

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Order cannot be paid in current status: CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/payments - should return 401 when unauthenticated")
    void processPayment_Unauthenticated_ReturnsUnauthorized() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "SUCCESS", null);

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/payments - should return 400 on validation failure (missing fields)")
    void processPayment_MissingFields_ReturnsBadRequest() throws Exception {
        UUID orderId = UUID.randomUUID();
        String jsonPayload = "{\"paymentNotes\": \"just note\"}";

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }
}

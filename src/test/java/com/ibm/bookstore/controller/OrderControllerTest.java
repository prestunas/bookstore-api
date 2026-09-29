package com.ibm.bookstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.bookstore.config.JwtProperties;
import com.ibm.bookstore.config.SecurityConfig;
import com.ibm.bookstore.dto.AddressDto;
import com.ibm.bookstore.dto.BookSummaryDto;
import com.ibm.bookstore.dto.CheckoutRequest;
import com.ibm.bookstore.dto.OrderItemResponse;
import com.ibm.bookstore.dto.OrderResponse;
import com.ibm.bookstore.dto.OrderSummaryDto;
import com.ibm.bookstore.entity.OrderStatus;
import com.ibm.bookstore.exception.BusinessRuleException;
import com.ibm.bookstore.exception.GlobalExceptionHandler;
import com.ibm.bookstore.exception.OrderCancellationExpiredException;
import com.ibm.bookstore.exception.ResourceNotFoundException;
import com.ibm.bookstore.security.JwtAuthenticationFilter;
import com.ibm.bookstore.security.JwtUtils;
import com.ibm.bookstore.service.OrderService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtils.class, JwtProperties.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "bookstore.security.jwt.secret=test-secret-key-minimum-32-bytes!!",
        "bookstore.security.jwt.expiration-ms=86400000"
})
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtProperties jwtProperties;

    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @MockitoBean
    OrderService orderService;

    String token;

    @BeforeEach
    void setUp() {
        JwtUtils jwtUtils = new JwtUtils(jwtProperties);
        token = jwtUtils.generateToken(UUID.randomUUID(), "johndoe", "ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("POST /api/v1/orders/checkout - should return 201 Created on valid checkout")
    void checkout_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        AddressDto addressDto = new AddressDto(
                addressId, "John Doe", "+1-555-0100", "123 Main St", "City", "State", "12345", "USA", true
        );

        OrderItemResponse itemResponse = new OrderItemResponse(
                itemId, bookId, "Clean Code", new BigDecimal("39.99"), 1, new BigDecimal("39.99")
        );

        OrderResponse response = new OrderResponse(
                orderId, "ORD-20250510-1234", OrderStatus.PENDING_PAYMENT,
                List.of(itemResponse), addressDto,
                new BigDecimal("39.99"), BigDecimal.ZERO, 0, 0,
                BigDecimal.ZERO, new BigDecimal("5.00"), new BigDecimal("44.99"),
                Instant.now(), true
        );

        when(orderService.checkout(eq("johndoe"), any(CheckoutRequest.class))).thenReturn(response);

        CheckoutRequest request = new CheckoutRequest(addressId, 0);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-20250510-1234"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount").value(44.99))
                .andExpect(jsonPath("$.items[0].bookTitle").value("Clean Code"))
                .andExpect(jsonPath("$.canBeCancelled").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/orders/checkout without auth - should return 401 Unauthorized")
    void checkout_Unauthorized() throws Exception {
        CheckoutRequest request = new CheckoutRequest(UUID.randomUUID(), 0);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/orders/checkout - excessive points returns 400 ProblemDetail")
    void checkout_ExcessivePoints_BadRequest() throws Exception {
        when(orderService.checkout(eq("johndoe"), any(CheckoutRequest.class)))
                .thenThrow(new BusinessRuleException("Requested points exceed available balance."));

        CheckoutRequest request = new CheckoutRequest(UUID.randomUUID(), 1000);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Requested points exceed available balance."));
    }

    @Test
    @DisplayName("GET /api/v1/orders - should return list of order summaries")
    void listUserOrders_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderSummaryDto summary = new OrderSummaryDto(
                orderId, "ORD-20250510-1234", OrderStatus.PAID,
                new BigDecimal("44.99"), Instant.now(), 1
        );

        when(orderService.getUserOrders("johndoe")).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-20250510-1234"))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].totalItems").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - should return order details")
    void getOrderById_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = new OrderResponse(
                orderId, "ORD-20250510-1234", OrderStatus.PENDING_PAYMENT,
                List.of(), null,
                new BigDecimal("39.99"), BigDecimal.ZERO, 0, 0,
                BigDecimal.ZERO, new BigDecimal("5.00"), new BigDecimal("44.99"),
                Instant.now(), true
        );

        when(orderService.getOrderById("johndoe", orderId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-20250510-1234"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - not found should return 404 ProblemDetail")
    void getOrderById_NotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getOrderById("johndoe", orderId))
                .thenThrow(new ResourceNotFoundException("Order", orderId));

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/cancel - should cancel within 48h")
    void cancelOrder_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = new OrderResponse(
                orderId, "ORD-20250510-1234", OrderStatus.CANCELLED,
                List.of(), null,
                new BigDecimal("39.99"), BigDecimal.ZERO, 0, 0,
                BigDecimal.ZERO, new BigDecimal("5.00"), new BigDecimal("44.99"),
                Instant.now(), false
        );

        when(orderService.cancelOrder("johndoe", orderId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.canBeCancelled").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/cancel - expired beyond 48h returns 400 ProblemDetail")
    void cancelOrder_Expired_BadRequest() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.cancelOrder("johndoe", orderId))
                .thenThrow(new OrderCancellationExpiredException("Orders cannot be cancelled after 48 hours."));

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Orders cannot be cancelled after 48 hours."));
    }

    @Test
    @DisplayName("GET /api/v1/orders/buy-again - returns list of past purchased books")
    void getBuyAgainBooks_Success() throws Exception {
        UUID bookId = UUID.randomUUID();
        BookSummaryDto bookSummary = new BookSummaryDto(
                bookId, "Clean Code", "978-0132350884",
                new BigDecimal("39.99"), 10, "https://example.com/cover.jpg",
                "Robert C. Martin", "Technology", 2
        );

        when(orderService.getBuyAgainBooks("johndoe")).thenReturn(List.of(bookSummary));

        mockMvc.perform(get("/api/v1/orders/buy-again")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookId.toString()))
                .andExpect(jsonPath("$[0].title").value("Clean Code"));
    }
}

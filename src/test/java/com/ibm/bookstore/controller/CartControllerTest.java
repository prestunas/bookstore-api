package com.ibm.bookstore.controller;

import com.ibm.bookstore.dto.AddCartItemRequest;
import com.ibm.bookstore.dto.CartItemResponse;
import com.ibm.bookstore.dto.CartResponse;
import com.ibm.bookstore.dto.UpdateCartItemRequest;
import com.ibm.bookstore.exception.InsufficientStockException;
import com.ibm.bookstore.exception.ResourceNotFoundException;
import com.ibm.bookstore.security.JwtUtils;
import com.ibm.bookstore.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ibm.bookstore.config.JwtProperties;
import com.ibm.bookstore.config.SecurityConfig;
import com.ibm.bookstore.exception.GlobalExceptionHandler;
import com.ibm.bookstore.security.JwtAuthenticationFilter;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtils.class, JwtProperties.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "bookstore.security.jwt.secret=test-secret-key-minimum-32-bytes!!",
        "bookstore.security.jwt.expiration-ms=86400000"
})
class CartControllerTest {

    @Autowired
    MockMvc mockMvc;

    static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    @MockitoBean
    CartService cartService;

    @Test
    @WithMockUser(username = "johndoe")
    @DisplayName("GET /api/v1/cart - should return 200 with cart response")
    void getCart_Success() throws Exception {
        UUID cartId = UUID.randomUUID();
        CartResponse response = new CartResponse(cartId, List.of(), 0, BigDecimal.ZERO);
        when(cartService.getCart("johndoe")).thenReturn(response);

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.totalQuantity").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/cart without auth - should return 401 Unauthorized")
    void getCart_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "johndoe")
    @DisplayName("POST /api/v1/cart/items - should return 200 with updated cart")
    void addItem_Success() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        CartItemResponse itemResponse = new CartItemResponse(
                itemId, bookId, "Clean Code", "978-0132350884", null,
                new BigDecimal("39.99"), 2, new BigDecimal("79.98"), 3
        );
        CartResponse response = new CartResponse(cartId, List.of(itemResponse), 2, new BigDecimal("79.98"));

        when(cartService.addItem(eq("johndoe"), any(AddCartItemRequest.class))).thenReturn(response);

        AddCartItemRequest request = new AddCartItemRequest(bookId, 2);

        mockMvc.perform(post("/api/v1/cart/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.items[0].bookTitle").value("Clean Code"))
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.totalAmount").value(79.98));
    }

    @Test
    @WithMockUser(username = "johndoe")
    @DisplayName("POST /api/v1/cart/items - insufficient stock should return 400 ProblemDetail")
    void addItem_InsufficientStock() throws Exception {
        UUID bookId = UUID.randomUUID();
        when(cartService.addItem(eq("johndoe"), any(AddCartItemRequest.class)))
                .thenThrow(new InsufficientStockException("Requested quantity exceeds available stock"));

        AddCartItemRequest request = new AddCartItemRequest(bookId, 50);

        mockMvc.perform(post("/api/v1/cart/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Requested quantity exceeds available stock"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    @DisplayName("PUT /api/v1/cart/items/{itemId} - should return 200 with updated cart")
    void updateItemQuantity_Success() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        CartResponse response = new CartResponse(cartId, List.of(), 5, new BigDecimal("199.95"));

        when(cartService.updateItemQuantity(eq("johndoe"), eq(itemId), any(UpdateCartItemRequest.class)))
                .thenReturn(response);

        UpdateCartItemRequest request = new UpdateCartItemRequest(5);

        mockMvc.perform(put("/api/v1/cart/items/{itemId}", itemId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(5));
    }

    @Test
    @WithMockUser(username = "johndoe")
    @DisplayName("DELETE /api/v1/cart/items/{itemId} - should return 200 with updated cart")
    void removeItem_Success() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        CartResponse response = new CartResponse(cartId, List.of(), 0, BigDecimal.ZERO);

        when(cartService.removeItem("johndoe", itemId)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/cart/items/{itemId}", itemId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(0));
    }

    @Test
    @WithMockUser(username = "johndoe")
    @DisplayName("DELETE /api/v1/cart/items/{itemId} - not found should return 404 ProblemDetail")
    void removeItem_NotFound() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(cartService.removeItem("johndoe", itemId))
                .thenThrow(new ResourceNotFoundException("CartItem", itemId));

        mockMvc.perform(delete("/api/v1/cart/items/{itemId}", itemId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    @DisplayName("DELETE /api/v1/cart - should return 204 No Content")
    void clearCart_Success() throws Exception {
        doNothing().when(cartService).clearCart("johndoe");

        mockMvc.perform(delete("/api/v1/cart")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}

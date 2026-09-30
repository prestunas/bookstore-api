package com.ibm.bookstore.controller;

import com.ibm.bookstore.config.JwtProperties;
import com.ibm.bookstore.config.SecurityConfig;
import com.ibm.bookstore.dto.BookSummaryDto;
import com.ibm.bookstore.exception.GlobalExceptionHandler;
import com.ibm.bookstore.security.JwtAuthenticationFilter;
import com.ibm.bookstore.security.JwtUtils;
import com.ibm.bookstore.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtils.class, JwtProperties.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "bookstore.security.jwt.secret=test-secret-key-minimum-32-bytes!!",
        "bookstore.security.jwt.expiration-ms=86400000"
})
class RecommendationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtProperties jwtProperties;

    @MockitoBean
    RecommendationService recommendationService;

    String token;

    @BeforeEach
    void setUp() {
        JwtUtils jwtUtils = new JwtUtils(jwtProperties);
        token = jwtUtils.generateToken(UUID.randomUUID(), "johndoe", "ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/order-history - 200 with recommendation list")
    void getOrderHistoryRecommendations_Success() throws Exception {
        UUID bookId = UUID.randomUUID();
        BookSummaryDto recommendation = new BookSummaryDto(
                bookId, "Clean Code", "978-0132350884",
                BigDecimal.valueOf(39.99), 10, "https://example.com/cover.jpg",
                "Robert C. Martin", "Software Engineering", 3
        );

        when(recommendationService.getOrderHistoryRecommendations(eq("johndoe"), anyInt()))
                .thenReturn(List.of(recommendation));

        mockMvc.perform(get("/api/v1/recommendations/order-history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookId.toString()))
                .andExpect(jsonPath("$[0].title").value("Clean Code"))
                .andExpect(jsonPath("$[0].authorName").value("Robert C. Martin"))
                .andExpect(jsonPath("$[0].categoryName").value("Software Engineering"));
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/order-history - 200 empty list when no history and no books")
    void getOrderHistoryRecommendations_EmptyList() throws Exception {
        when(recommendationService.getOrderHistoryRecommendations(eq("johndoe"), anyInt()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/recommendations/order-history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/order-history?limit=3 - respects custom limit parameter")
    void getOrderHistoryRecommendations_CustomLimit() throws Exception {
        when(recommendationService.getOrderHistoryRecommendations(eq("johndoe"), eq(3)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/recommendations/order-history")
                        .param("limit", "3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/recommendations/order-history - 401 when unauthenticated")
    void getOrderHistoryRecommendations_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/order-history"))
                .andExpect(status().isUnauthorized());
    }
}

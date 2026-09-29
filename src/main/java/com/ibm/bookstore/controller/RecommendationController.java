package com.ibm.bookstore.controller;

import com.ibm.bookstore.dto.BookSummaryDto;
import com.ibm.bookstore.service.RecommendationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecommendationController {

    RecommendationService recommendationService;

    @GetMapping("/order-history")
    public List<BookSummaryDto> getOrderHistoryRecommendations(
            @AuthenticationPrincipal String username,
            @RequestParam(defaultValue = "6") int limit
    ) {
        return recommendationService.getOrderHistoryRecommendations(username, limit);
    }
}

package com.ibm.bookstore.controller;

import com.ibm.bookstore.dto.BookSummaryDto;
import com.ibm.bookstore.dto.CheckoutRequest;
import com.ibm.bookstore.dto.OrderResponse;
import com.ibm.bookstore.dto.OrderSummaryDto;
import com.ibm.bookstore.service.OrderService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {

    OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody CheckoutRequest request
    ) {
        return orderService.checkout(username, request);
    }

    @GetMapping
    public List<OrderSummaryDto> listUserOrders(@AuthenticationPrincipal String username) {
        return orderService.getUserOrders(username);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrderById(
            @AuthenticationPrincipal String username,
            @PathVariable UUID orderId
    ) {
        return orderService.getOrderById(username, orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(
            @AuthenticationPrincipal String username,
            @PathVariable UUID orderId
    ) {
        return orderService.cancelOrder(username, orderId);
    }

    @GetMapping("/buy-again")
    public List<BookSummaryDto> getBuyAgainBooks(@AuthenticationPrincipal String username) {
        return orderService.getBuyAgainBooks(username);
    }
}

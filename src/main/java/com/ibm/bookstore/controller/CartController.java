package com.ibm.bookstore.controller;

import com.ibm.bookstore.dto.AddCartItemRequest;
import com.ibm.bookstore.dto.CartResponse;
import com.ibm.bookstore.dto.UpdateCartItemRequest;
import com.ibm.bookstore.service.CartService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartController {

    CartService cartService;

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal String username) {
        return cartService.getCart(username);
    }

    @PostMapping("/items")
    public CartResponse addItem(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return cartService.addItem(username, request);
    }

    @PutMapping("/items/{itemId}")
    public CartResponse updateItemQuantity(
            @AuthenticationPrincipal String username,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItemQuantity(username, itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(
            @AuthenticationPrincipal String username,
            @PathVariable UUID itemId
    ) {
        return cartService.removeItem(username, itemId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@AuthenticationPrincipal String username) {
        cartService.clearCart(username);
    }
}

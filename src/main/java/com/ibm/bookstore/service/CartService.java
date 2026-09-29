package com.ibm.bookstore.service;

import com.ibm.bookstore.dto.AddCartItemRequest;
import com.ibm.bookstore.dto.CartItemResponse;
import com.ibm.bookstore.dto.CartResponse;
import com.ibm.bookstore.dto.UpdateCartItemRequest;
import com.ibm.bookstore.entity.Book;
import com.ibm.bookstore.entity.Cart;
import com.ibm.bookstore.entity.CartItem;
import com.ibm.bookstore.entity.User;
import com.ibm.bookstore.exception.InsufficientStockException;
import com.ibm.bookstore.exception.ResourceNotFoundException;
import com.ibm.bookstore.repository.BookRepository;
import com.ibm.bookstore.repository.CartRepository;
import com.ibm.bookstore.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartService {

    CartRepository cartRepository;
    BookRepository bookRepository;
    UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(String username) {
        User user = getUser(username);
        Cart cart = getOrCreateCart(user);
        return toCartResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String username, AddCartItemRequest request) {
        User user = getUser(username);
        Cart cart = getOrCreateCart(user);

        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", request.bookId()));

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getBook().getId().equals(book.getId()))
                .findFirst()
                .orElse(null);

        int newQuantity = (existingItem != null ? existingItem.getQuantity() : 0) + request.quantity();

        if (book.getStockQuantity() < newQuantity) {
            throw new InsufficientStockException(
                    "Requested quantity (" + newQuantity + ") exceeds available stock (" + book.getStockQuantity() + ") for book: " + book.getTitle()
            );
        }

        if (existingItem != null) {
            existingItem.setQuantity(newQuantity);
        } else {
            CartItem newItem = CartItem.builder()
                    .id(UUID.randomUUID())
                    .cart(cart)
                    .book(book)
                    .quantity(request.quantity())
                    .build();
            cart.getItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return toCartResponse(savedCart);
    }

    @Transactional
    public CartResponse updateItemQuantity(String username, UUID itemId, UpdateCartItemRequest request) {
        User user = getUser(username);
        Cart cart = getOrCreateCart(user);

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        Book book = cartItem.getBook();
        if (book.getStockQuantity() < request.quantity()) {
            throw new InsufficientStockException(
                    "Requested quantity (" + request.quantity() + ") exceeds available stock (" + book.getStockQuantity() + ") for book: " + book.getTitle()
            );
        }

        cartItem.setQuantity(request.quantity());
        Cart savedCart = cartRepository.save(cart);
        return toCartResponse(savedCart);
    }

    @Transactional
    public CartResponse removeItem(String username, UUID itemId) {
        User user = getUser(username);
        Cart cart = getOrCreateCart(user);

        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new ResourceNotFoundException("CartItem", itemId);
        }

        Cart savedCart = cartRepository.save(cart);
        return toCartResponse(savedCart);
    }

    @Transactional
    public void clearCart(String username) {
        User user = getUser(username);
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }

    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .id(UUID.randomUUID())
                            .user(user)
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::toCartItemResponse)
                .toList();

        int totalQuantity = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        BigDecimal totalAmount = cart.getItems().stream()
                .map(item -> item.getBook().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), itemResponses, totalQuantity, totalAmount);
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        Book book = item.getBook();
        BigDecimal subtotal = book.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

        return new CartItemResponse(
                item.getId(),
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getCoverImageUrl(),
                book.getPrice(),
                item.getQuantity(),
                subtotal,
                book.getExpectedDeliveryDays()
        );
    }
}

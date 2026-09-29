package com.ibm.bookstore.service;

import com.ibm.bookstore.config.BookstoreProperties;
import com.ibm.bookstore.dto.AddressDto;
import com.ibm.bookstore.dto.BookSummaryDto;
import com.ibm.bookstore.dto.CheckoutRequest;
import com.ibm.bookstore.dto.OrderItemResponse;
import com.ibm.bookstore.dto.OrderResponse;
import com.ibm.bookstore.dto.OrderSummaryDto;
import com.ibm.bookstore.entity.Book;
import com.ibm.bookstore.entity.Cart;
import com.ibm.bookstore.entity.CartItem;
import com.ibm.bookstore.entity.Order;
import com.ibm.bookstore.entity.OrderItem;
import com.ibm.bookstore.entity.OrderStatus;
import com.ibm.bookstore.entity.User;
import com.ibm.bookstore.entity.UserAddress;
import com.ibm.bookstore.exception.BusinessRuleException;
import com.ibm.bookstore.exception.InsufficientStockException;
import com.ibm.bookstore.exception.OrderCancellationExpiredException;
import com.ibm.bookstore.exception.ResourceNotFoundException;
import com.ibm.bookstore.repository.BookRepository;
import com.ibm.bookstore.repository.CartRepository;
import com.ibm.bookstore.repository.OrderRepository;
import com.ibm.bookstore.repository.UserAddressRepository;
import com.ibm.bookstore.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {

    OrderRepository orderRepository;
    CartRepository cartRepository;
    BookRepository bookRepository;
    UserRepository userRepository;
    UserAddressRepository userAddressRepository;
    CatalogService catalogService;
    BookstoreProperties bookstoreProperties;
    Clock clock;

    @Transactional
    public OrderResponse checkout(String username, CheckoutRequest request) {
        User user = getUser(username);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessRuleException("Shopping cart is empty."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessRuleException("Shopping cart is empty.");
        }

        UserAddress shippingAddress = userAddressRepository.findById(request.addressId())
                .orElseThrow(() -> new ResourceNotFoundException("UserAddress", request.addressId()));

        if (!shippingAddress.getUser().getId().equals(user.getId())) {
            throw new BusinessRuleException("The specified shipping address does not belong to the current user.");
        }

        // Validate stock for all items
        for (CartItem cartItem : cart.getItems()) {
            Book book = cartItem.getBook();
            if (book.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for book '" + book.getTitle() + "'. Available: "
                                + book.getStockQuantity() + ", requested: " + cartItem.getQuantity()
                );
            }
        }

        // Calculate subtotal
        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getBook().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Calculate gift points redemption and discount
        int requestedPoints = request.giftPointsToRedeem() != null ? request.giftPointsToRedeem() : 0;
        if (requestedPoints > user.getRewardPoints()) {
            throw new BusinessRuleException(
                    "Requested points (" + requestedPoints + ") exceed available balance (" + user.getRewardPoints() + ")."
            );
        }

        int rate = bookstoreProperties.getGiftPoints().getRate();
        BigDecimal maxPointsRedeemableForSubtotal = subtotal.multiply(BigDecimal.valueOf(rate));
        if (BigDecimal.valueOf(requestedPoints).compareTo(maxPointsRedeemableForSubtotal) > 0) {
            throw new BusinessRuleException("Redeemed points value cannot exceed the subtotal amount.");
        }

        BigDecimal discountAmount = BigDecimal.valueOf(requestedPoints)
                .divide(BigDecimal.valueOf(rate), 2, RoundingMode.HALF_UP);

        // Shipping calculation
        BigDecimal freeThreshold = bookstoreProperties.getShipping().getFreeThreshold();
        BigDecimal flatRate = bookstoreProperties.getShipping().getFlatRate();
        BigDecimal shippingAmount = (subtotal.compareTo(freeThreshold) >= 0)
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : flatRate.setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.subtract(discountAmount).add(taxAmount).add(shippingAmount)
                .setScale(2, RoundingMode.HALF_UP);

        Instant now = clock.instant();
        String orderNumber = generateOrderNumber(now);

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber(orderNumber)
                .user(user)
                .status(OrderStatus.PENDING_PAYMENT)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .pointsRedeemed(requestedPoints)
                .pointsEarned(0)
                .taxAmount(taxAmount)
                .shippingAmount(shippingAmount)
                .totalAmount(totalAmount)
                .orderedAt(now)
                .recipientName(shippingAddress.getRecipientName())
                .phone(shippingAddress.getPhone())
                .street(shippingAddress.getStreet())
                .city(shippingAddress.getCity())
                .state(shippingAddress.getState())
                .postalCode(shippingAddress.getPostalCode())
                .country(shippingAddress.getCountry())
                .items(new ArrayList<>())
                .build();

        for (CartItem cartItem : cart.getItems()) {
            Book book = cartItem.getBook();
            BigDecimal itemSubtotal = book.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItem orderItem = OrderItem.builder()
                    .id(UUID.randomUUID())
                    .order(order)
                    .book(book)
                    .bookTitle(book.getTitle())
                    .unitPrice(book.getPrice())
                    .quantity(cartItem.getQuantity())
                    .subtotal(itemSubtotal)
                    .build();

            order.getItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);
        return toOrderResponse(savedOrder, shippingAddress.getId());
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryDto> getUserOrders(String username) {
        User user = getUser(username);
        return orderRepository.findByUserIdOrderByOrderedAtDesc(user.getId()).stream()
                .map(this::toOrderSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String username, UUID orderId) {
        User user = getUser(username);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order", orderId);
        }

        return toOrderResponse(order, null);
    }

    @Transactional
    public OrderResponse cancelOrder(String username, UUID orderId) {
        User user = getUser(username);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order", orderId);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Order is already cancelled.");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Delivered orders cannot be cancelled.");
        }

        int windowHours = bookstoreProperties.getCancellation().getWindowHours();
        Instant expiryTime = order.getOrderedAt().plus(windowHours, ChronoUnit.HOURS);
        if (clock.instant().isAfter(expiryTime)) {
            throw new OrderCancellationExpiredException(
                    "Orders cannot be cancelled after " + windowHours + " hours from placement time."
            );
        }

        // If order was PAID, restore book inventory, refund redeemed points, and reverse earned points exactly once
        if (order.getStatus() == OrderStatus.PAID) {
            for (OrderItem item : order.getItems()) {
                Book book = item.getBook();
                book.setStockQuantity(book.getStockQuantity() + item.getQuantity());
                bookRepository.save(book);
            }
            int pointsToRefund = order.getPointsRedeemed() != null ? order.getPointsRedeemed() : 0;
            int pointsToReverse = order.getPointsEarned() != null ? order.getPointsEarned() : 0;
            if (pointsToRefund > 0 || pointsToReverse > 0) {
                int updatedPoints = Math.max(0, user.getRewardPoints() + pointsToRefund - pointsToReverse);
                user.setRewardPoints(updatedPoints);
                userRepository.save(user);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        return toOrderResponse(savedOrder, null);
    }

    @Transactional(readOnly = true)
    public List<BookSummaryDto> getBuyAgainBooks(String username) {
        User user = getUser(username);
        return bookRepository.findBuyAgainBooksByUser(user.getId()).stream()
                .map(catalogService::toBookSummaryDto)
                .toList();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }

    private String generateOrderNumber(Instant now) {
        String hexSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + now.toEpochMilli() + "-" + hexSuffix;
    }

    private boolean isCancellable(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            return false;
        }
        int windowHours = bookstoreProperties.getCancellation().getWindowHours();
        Instant expiryTime = order.getOrderedAt().plus(windowHours, ChronoUnit.HOURS);
        return !clock.instant().isAfter(expiryTime);
    }

    private OrderResponse toOrderResponse(Order order, UUID addressId) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getBook().getId(),
                        item.getBookTitle(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .toList();

        AddressDto addressDto = new AddressDto(
                addressId,
                order.getRecipientName(),
                order.getPhone(),
                order.getStreet(),
                order.getCity(),
                order.getState(),
                order.getPostalCode(),
                order.getCountry(),
                false
        );

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                itemResponses,
                addressDto,
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getPointsRedeemed(),
                order.getPointsEarned(),
                order.getTaxAmount(),
                order.getShippingAmount(),
                order.getTotalAmount(),
                order.getOrderedAt(),
                isCancellable(order)
        );
    }

    private OrderSummaryDto toOrderSummaryDto(Order order) {
        int totalItems = order.getItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        return new OrderSummaryDto(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getOrderedAt(),
                totalItems
        );
    }
}

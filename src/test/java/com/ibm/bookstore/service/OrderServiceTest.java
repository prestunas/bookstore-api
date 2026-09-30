package com.ibm.bookstore.service;

import com.ibm.bookstore.config.BookstoreProperties;
import com.ibm.bookstore.dto.BookSummaryDto;
import com.ibm.bookstore.dto.CheckoutRequest;
import com.ibm.bookstore.dto.OrderResponse;
import com.ibm.bookstore.dto.OrderSummaryDto;
import com.ibm.bookstore.entity.Author;
import com.ibm.bookstore.entity.Book;
import com.ibm.bookstore.entity.Cart;
import com.ibm.bookstore.entity.CartItem;
import com.ibm.bookstore.entity.Category;
import com.ibm.bookstore.entity.Order;
import com.ibm.bookstore.entity.OrderItem;
import com.ibm.bookstore.entity.OrderStatus;
import com.ibm.bookstore.entity.Publisher;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    CartRepository cartRepository;

    @Mock
    BookRepository bookRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    UserAddressRepository userAddressRepository;

    @Mock
    CatalogService catalogService;

    BookstoreProperties bookstoreProperties;
    Clock fixedClock;
    Instant fixedInstant;

    OrderService orderService;

    User testUser;
    UserAddress testAddress;
    Book testBook1;
    Book testBook2;
    Cart testCart;

    @BeforeEach
    void setUp() {
        bookstoreProperties = new BookstoreProperties();
        bookstoreProperties.getGiftPoints().setRate(100);
        bookstoreProperties.getShipping().setFlatRate(new BigDecimal("5.00"));
        bookstoreProperties.getShipping().setFreeThreshold(new BigDecimal("50.00"));
        bookstoreProperties.getCancellation().setWindowHours(48);

        fixedInstant = Instant.parse("2025-05-10T12:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

        orderService = new OrderService(
                orderRepository,
                cartRepository,
                bookRepository,
                userRepository,
                userAddressRepository,
                catalogService,
                bookstoreProperties,
                fixedClock
        );

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .rewardPoints(500)
                .build();

        testAddress = UserAddress.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .recipientName("Test User")
                .phone("+1-555-0100")
                .street("123 Main St")
                .city("Springfield")
                .state("IL")
                .postalCode("62701")
                .country("USA")
                .isDefault(true)
                .build();

        Category category = Category.builder().id(UUID.randomUUID()).name("Tech").build();
        Author author = Author.builder().id(UUID.randomUUID()).name("Author").build();
        Publisher publisher = Publisher.builder().id(UUID.randomUUID()).name("Pub").build();

        testBook1 = Book.builder()
                .id(UUID.randomUUID())
                .title("Clean Architecture")
                .isbn("978-0134494166")
                .price(new BigDecimal("30.00"))
                .stockQuantity(10)
                .category(category)
                .author(author)
                .publisher(publisher)
                .build();

        testBook2 = Book.builder()
                .id(UUID.randomUUID())
                .title("Clean Code")
                .isbn("978-0132350884")
                .price(new BigDecimal("25.00"))
                .stockQuantity(5)
                .category(category)
                .author(author)
                .publisher(publisher)
                .build();

        testCart = Cart.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .items(new ArrayList<>())
                .build();
    }

    // ---- Checkout Tests ----

    @Test
    @DisplayName("checkout - below free shipping threshold without points redemption")
    void checkout_BelowFreeShipping_NoPoints() {
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID())
                .cart(testCart)
                .book(testBook1)
                .quantity(1)
                .build();
        testCart.getItems().add(item);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(userAddressRepository.findById(testAddress.getId())).thenReturn(Optional.of(testAddress));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = new CheckoutRequest(testAddress.getId(), 0);
        OrderResponse response = orderService.checkout("testuser", request);

        assertThat(response).isNotNull();
        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(response.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.shippingAmount()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("35.00"));
        assertThat(response.pointsRedeemed()).isEqualTo(0);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(response.items()).hasSize(1);
        assertThat(response.canBeCancelled()).isTrue();
    }

    @Test
    @DisplayName("checkout - above free shipping threshold ($55 subtotal >= $50 threshold)")
    void checkout_AboveFreeShipping() {
        CartItem item1 = CartItem.builder().id(UUID.randomUUID()).cart(testCart).book(testBook1).quantity(1).build();
        CartItem item2 = CartItem.builder().id(UUID.randomUUID()).cart(testCart).book(testBook2).quantity(1).build();
        testCart.getItems().add(item1);
        testCart.getItems().add(item2);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(userAddressRepository.findById(testAddress.getId())).thenReturn(Optional.of(testAddress));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = new CheckoutRequest(testAddress.getId(), 0);
        OrderResponse response = orderService.checkout("testuser", request);

        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("55.00"));
        assertThat(response.shippingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("55.00"));
    }

    @Test
    @DisplayName("checkout - with valid gift points redemption (200 points = $2.00 discount)")
    void checkout_WithGiftPointsRedemption() {
        CartItem item = CartItem.builder().id(UUID.randomUUID()).cart(testCart).book(testBook1).quantity(2).build(); // subtotal: $60
        testCart.getItems().add(item);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(userAddressRepository.findById(testAddress.getId())).thenReturn(Optional.of(testAddress));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = new CheckoutRequest(testAddress.getId(), 200);
        OrderResponse response = orderService.checkout("testuser", request);

        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(response.discountAmount()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(response.pointsRedeemed()).isEqualTo(200);
        assertThat(response.shippingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("58.00"));
    }

    @Test
    @DisplayName("checkout - should throw BusinessRuleException when gift points exceed user balance")
    void checkout_ExcessiveGiftPoints_ExceedsBalance() {
        CartItem item = CartItem.builder().id(UUID.randomUUID()).cart(testCart).book(testBook1).quantity(1).build();
        testCart.getItems().add(item);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(userAddressRepository.findById(testAddress.getId())).thenReturn(Optional.of(testAddress));

        CheckoutRequest request = new CheckoutRequest(testAddress.getId(), 1000); // user only has 500

        assertThatThrownBy(() -> orderService.checkout("testuser", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceed available balance");
    }

    @Test
    @DisplayName("checkout - should throw BusinessRuleException when gift points exceed order subtotal value")
    void checkout_ExcessiveGiftPoints_ExceedsSubtotal() {
        testUser.setRewardPoints(10000);
        testBook1.setPrice(new BigDecimal("5.00")); // Subtotal = $5.00 = max 500 points
        CartItem item = CartItem.builder().id(UUID.randomUUID()).cart(testCart).book(testBook1).quantity(1).build();
        testCart.getItems().add(item);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(userAddressRepository.findById(testAddress.getId())).thenReturn(Optional.of(testAddress));

        CheckoutRequest request = new CheckoutRequest(testAddress.getId(), 600); // $6.00 > $5.00

        assertThatThrownBy(() -> orderService.checkout("testuser", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot exceed the subtotal");
    }

    @Test
    @DisplayName("checkout - should throw BusinessRuleException when cart is empty")
    void checkout_EmptyCart() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

        CheckoutRequest request = new CheckoutRequest(testAddress.getId(), 0);

        assertThatThrownBy(() -> orderService.checkout("testuser", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Shopping cart is empty");
    }

    @Test
    @DisplayName("checkout - should throw BusinessRuleException when address belongs to another user")
    void checkout_AddressBelongsToAnotherUser() {
        CartItem item = CartItem.builder().id(UUID.randomUUID()).cart(testCart).book(testBook1).quantity(1).build();
        testCart.getItems().add(item);

        User anotherUser = User.builder().id(UUID.randomUUID()).username("other").build();
        UserAddress otherAddress = UserAddress.builder().id(UUID.randomUUID()).user(anotherUser).build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(userAddressRepository.findById(otherAddress.getId())).thenReturn(Optional.of(otherAddress));

        CheckoutRequest request = new CheckoutRequest(otherAddress.getId(), 0);

        assertThatThrownBy(() -> orderService.checkout("testuser", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("does not belong to the current user");
    }

    @Test
    @DisplayName("checkout - should throw InsufficientStockException when item stock is insufficient")
    void checkout_InsufficientStock() {
        testBook1.setStockQuantity(2);
        CartItem item = CartItem.builder().id(UUID.randomUUID()).cart(testCart).book(testBook1).quantity(5).build();
        testCart.getItems().add(item);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(userAddressRepository.findById(testAddress.getId())).thenReturn(Optional.of(testAddress));

        CheckoutRequest request = new CheckoutRequest(testAddress.getId(), 0);

        assertThatThrownBy(() -> orderService.checkout("testuser", request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
    }

    // ---- Order Details & History Tests ----

    @Test
    @DisplayName("getOrderById - should return order when user is owner")
    void getOrderById_Owner_Success() {
        Order order = createSampleOrder(testUser, fixedInstant.minus(1, ChronoUnit.HOURS), OrderStatus.PENDING_PAYMENT);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById("testuser", order.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(order.getId());
        assertThat(response.canBeCancelled()).isTrue();
    }

    @Test
    @DisplayName("getOrderById - should throw ResourceNotFoundException when user is not owner")
    void getOrderById_NotOwner_ThrowsException() {
        User otherUser = User.builder().id(UUID.randomUUID()).username("other").build();
        Order order = createSampleOrder(otherUser, fixedInstant.minus(1, ChronoUnit.HOURS), OrderStatus.PENDING_PAYMENT);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById("testuser", order.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getUserOrders - should return summary list sorted by orderedAt desc")
    void getUserOrders_Success() {
        Order order = createSampleOrder(testUser, fixedInstant.minus(2, ChronoUnit.HOURS), OrderStatus.PAID);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUserIdOrderByOrderedAtDesc(testUser.getId())).thenReturn(List.of(order));

        List<OrderSummaryDto> orders = orderService.getUserOrders("testuser");

        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().orderNumber()).isEqualTo(order.getOrderNumber());
        assertThat(orders.getFirst().totalItems()).isEqualTo(2);
    }

    // ---- Exact 48-Hour Cancellation Boundary Tests ----

    @Test
    @DisplayName("cancelOrder - exactly at 47 hours and 59 minutes (within 48h) should succeed")
    void cancelOrder_Within48Hours_Success() {
        Instant orderedAt = fixedInstant.minus(47, ChronoUnit.HOURS).minus(59, ChronoUnit.MINUTES);
        Order order = createSampleOrder(testUser, orderedAt, OrderStatus.PENDING_PAYMENT);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder("testuser", order.getId());

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.canBeCancelled()).isFalse();
    }

    @Test
    @DisplayName("cancelOrder - exactly at 48 hours boundary should succeed")
    void cancelOrder_ExactlyAt48HoursBoundary_Success() {
        // orderedAt + 48 hours == fixedInstant -> isAfter is false -> allowed
        Instant orderedAt = fixedInstant.minus(48, ChronoUnit.HOURS);
        Order order = createSampleOrder(testUser, orderedAt, OrderStatus.PENDING_PAYMENT);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder("testuser", order.getId());

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelOrder - at 48 hours and 1 second should throw OrderCancellationExpiredException")
    void cancelOrder_ExpiredBeyond48Hours_ThrowsException() {
        Instant orderedAt = fixedInstant.minus(48, ChronoUnit.HOURS).minus(1, ChronoUnit.SECONDS);
        Order order = createSampleOrder(testUser, orderedAt, OrderStatus.PENDING_PAYMENT);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("testuser", order.getId()))
                .isInstanceOf(OrderCancellationExpiredException.class)
                .hasMessageContaining("cannot be cancelled after 48 hours");
    }

    @Test
    @DisplayName("cancelOrder - PAID order cancelled within 48h restores stock, refunds redeemed points, and reverses earned points")
    void cancelOrder_PaidOrder_RestoresStockAndPoints() {
        Instant orderedAt = fixedInstant.minus(1, ChronoUnit.HOURS);
        testBook1.setStockQuantity(5);
        testUser.setRewardPoints(100);

        Order order = createSampleOrder(testUser, orderedAt, OrderStatus.PAID);
        order.setPointsRedeemed(200);
        order.setPointsEarned(50);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder("testuser", order.getId());

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        // Stock should be incremented by item quantity (2) -> 5 + 2 = 7
        assertThat(testBook1.getStockQuantity()).isEqualTo(7);
        verify(bookRepository).save(testBook1);
        // Points refunded (200) and earned reversed (50): 100 + 200 - 50 = 250
        assertThat(testUser.getRewardPoints()).isEqualTo(250);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("cancelOrder - PENDING_PAYMENT order cancelled within 48h does not alter stock or reward points")
    void cancelOrder_PendingPaymentOrder_DoesNotAlterStockOrPoints() {
        Instant orderedAt = fixedInstant.minus(1, ChronoUnit.HOURS);
        testBook1.setStockQuantity(5);
        testUser.setRewardPoints(100);

        Order order = createSampleOrder(testUser, orderedAt, OrderStatus.PENDING_PAYMENT);
        order.setPointsRedeemed(200);
        order.setPointsEarned(0);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder("testuser", order.getId());

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        // Stock and points should not change for PENDING_PAYMENT order cancellation
        assertThat(testBook1.getStockQuantity()).isEqualTo(5);
        assertThat(testUser.getRewardPoints()).isEqualTo(100);
        verify(bookRepository, never()).save(any(Book.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("cancelOrder - already cancelled order throws BusinessRuleException")
    void cancelOrder_AlreadyCancelled_ThrowsException() {
        Order order = createSampleOrder(testUser, fixedInstant.minus(1, ChronoUnit.HOURS), OrderStatus.CANCELLED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("testuser", order.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    @DisplayName("cancelOrder - delivered order throws BusinessRuleException")
    void cancelOrder_Delivered_ThrowsException() {
        Order order = createSampleOrder(testUser, fixedInstant.minus(1, ChronoUnit.HOURS), OrderStatus.DELIVERED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("testuser", order.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Delivered orders cannot be cancelled");
    }

    // ---- Reward-Points Edge Cases ----

    @Test
    @DisplayName("cancelOrder - earned points from cancelled PAID order are reversed against current balance even when those points were already spent on another order")
    void cancelOrder_EarnedPointsAlreadySpentElsewhere_ReversedAgainstCurrentBalance() {
        /*
         * Demo rule (net-balance reversal):
         *  When a PAID order is cancelled, the system subtracts pointsEarned from the
         *  user's current balance. It does NOT attempt to trace whether those specific
         *  points were later spent on a different order. The balance floors at 0.
         *
         * Scenario:
         *  - Initial balance: 500 pts
         *  - Order A paid  : earned 80 pts   -> balance becomes 580
         *  - Order B paid  : redeemed 200 pts, earned 50 pts -> balance becomes 580 - 200 + 50 = 430
         *  - Cancel Order A: refund 0 redeemed, reverse 80 earned -> balance becomes Max(0, 430 - 80) = 350
         *
         * The reversal is applied to whatever the current balance is; there is no
         * per-order points ledger tracing. The Math.max(0, ...) guard prevents a
         * negative balance in all cases.
         */
        Instant orderedAt = fixedInstant.minus(1, ChronoUnit.HOURS);

        // After Order B was paid, user's current balance is 430
        testUser.setRewardPoints(430);

        // Order A: paid, earned 80 pts, nothing redeemed
        Order orderA = createSampleOrder(testUser, orderedAt, OrderStatus.PAID);
        orderA.setPointsRedeemed(0);
        orderA.setPointsEarned(80);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(orderA.getId())).thenReturn(Optional.of(orderA));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder("testuser", orderA.getId());

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        // Net-balance reversal: 430 + 0 (refund) - 80 (reverse earned) = 350
        assertThat(testUser.getRewardPoints()).isEqualTo(350);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("cancelOrder - reversing earned points floors at 0 when current balance is less than points earned")
    void cancelOrder_EarnedPointsExceedCurrentBalance_FloorAtZero() {
        /*
         * Edge case: user spent nearly all points after Order A was paid.
         * Current balance (5) < pointsEarned on Order A (80).
         * Math.max(0, 5 + 0 - 80) = 0 — balance must not go negative.
         */
        Instant orderedAt = fixedInstant.minus(1, ChronoUnit.HOURS);
        testUser.setRewardPoints(5);

        Order orderA = createSampleOrder(testUser, orderedAt, OrderStatus.PAID);
        orderA.setPointsRedeemed(0);
        orderA.setPointsEarned(80);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.findById(orderA.getId())).thenReturn(Optional.of(orderA));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder("testuser", orderA.getId());

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        // Floored at 0: Max(0, 5 - 80) = 0
        assertThat(testUser.getRewardPoints()).isEqualTo(0);
        verify(userRepository).save(testUser);
    }

    // ---- Buy Again Tests ----

    @Test
    @DisplayName("getBuyAgainBooks - returns distinct previously purchased books")
    void getBuyAgainBooks_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bookRepository.findBuyAgainBooksByUser(testUser.getId())).thenReturn(List.of(testBook1));
        when(catalogService.toBookSummaryDto(testBook1)).thenReturn(
                new BookSummaryDto(testBook1.getId(), "Clean Architecture", "978-0134494166",
                        new BigDecimal("30.00"), 10, null, "Author", "Tech", 2)
        );

        List<BookSummaryDto> buyAgain = orderService.getBuyAgainBooks("testuser");

        assertThat(buyAgain).hasSize(1);
        assertThat(buyAgain.getFirst().title()).isEqualTo("Clean Architecture");
    }

    private Order createSampleOrder(User user, Instant orderedAt, OrderStatus status) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-12345")
                .user(user)
                .status(status)
                .subtotal(new BigDecimal("60.00"))
                .discountAmount(BigDecimal.ZERO)
                .pointsRedeemed(0)
                .pointsEarned(0)
                .taxAmount(BigDecimal.ZERO)
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("60.00"))
                .orderedAt(orderedAt)
                .recipientName("Test User")
                .phone("+1-555-0100")
                .street("123 Main St")
                .city("Springfield")
                .state("IL")
                .postalCode("62701")
                .country("USA")
                .items(new ArrayList<>())
                .build();

        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .book(testBook1)
                .bookTitle(testBook1.getTitle())
                .unitPrice(testBook1.getPrice())
                .quantity(2)
                .subtotal(new BigDecimal("60.00"))
                .build();

        order.getItems().add(item);
        return order;
    }
}

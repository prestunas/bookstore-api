package com.ibm.bookstore.service;

import com.ibm.bookstore.dto.AddCartItemRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    CartRepository cartRepository;

    @Mock
    BookRepository bookRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    CartService cartService;

    User testUser;
    Book testBook;
    Cart testCart;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .build();

        testBook = Book.builder()
                .id(UUID.randomUUID())
                .title("Clean Code")
                .isbn("978-0132350884")
                .price(new BigDecimal("39.99"))
                .stockQuantity(10)
                .expectedDeliveryDays(3)
                .build();

        testCart = Cart.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .items(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("getCart - should return empty cart when no items")
    void getCart_Empty() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

        CartResponse response = cartService.getCart("testuser");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testCart.getId());
        assertThat(response.items()).isEmpty();
        assertThat(response.totalQuantity()).isEqualTo(0);
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("addItem - should add new item to cart when stock is sufficient")
    void addItem_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(bookRepository.findById(testBook.getId())).thenReturn(Optional.of(testBook));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddCartItemRequest request = new AddCartItemRequest(testBook.getId(), 2);
        CartResponse response = cartService.addItem("testuser", request);

        assertThat(response.items()).hasSize(1);
        assertThat(response.totalQuantity()).isEqualTo(2);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("79.98"));
        assertThat(response.items().getFirst().bookTitle()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("addItem - should throw InsufficientStockException when requested quantity exceeds stock")
    void addItem_InsufficientStock() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(bookRepository.findById(testBook.getId())).thenReturn(Optional.of(testBook));

        AddCartItemRequest request = new AddCartItemRequest(testBook.getId(), 15);

        assertThatThrownBy(() -> cartService.addItem("testuser", request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("exceeds available stock");
    }

    @Test
    @DisplayName("addItem - should increase quantity when item already in cart")
    void addItem_ExistingItem() {
        CartItem existingItem = CartItem.builder()
                .id(UUID.randomUUID())
                .cart(testCart)
                .book(testBook)
                .quantity(2)
                .build();
        testCart.getItems().add(existingItem);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(bookRepository.findById(testBook.getId())).thenReturn(Optional.of(testBook));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddCartItemRequest request = new AddCartItemRequest(testBook.getId(), 3);
        CartResponse response = cartService.addItem("testuser", request);

        assertThat(response.items()).hasSize(1);
        assertThat(response.totalQuantity()).isEqualTo(5);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("199.95"));
    }

    @Test
    @DisplayName("updateItemQuantity - should update quantity when valid")
    void updateItemQuantity_Success() {
        UUID itemId = UUID.randomUUID();
        CartItem item = CartItem.builder()
                .id(itemId)
                .cart(testCart)
                .book(testBook)
                .quantity(2)
                .build();
        testCart.getItems().add(item);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCartItemRequest request = new UpdateCartItemRequest(5);
        CartResponse response = cartService.updateItemQuantity("testuser", itemId, request);

        assertThat(response.totalQuantity()).isEqualTo(5);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("199.95"));
    }

    @Test
    @DisplayName("updateItemQuantity - should throw InsufficientStockException when quantity exceeds stock")
    void updateItemQuantity_InsufficientStock() {
        UUID itemId = UUID.randomUUID();
        CartItem item = CartItem.builder()
                .id(itemId)
                .cart(testCart)
                .book(testBook)
                .quantity(2)
                .build();
        testCart.getItems().add(item);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

        UpdateCartItemRequest request = new UpdateCartItemRequest(20);

        assertThatThrownBy(() -> cartService.updateItemQuantity("testuser", itemId, request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("exceeds available stock");
    }

    @Test
    @DisplayName("removeItem - should remove item from cart")
    void removeItem_Success() {
        UUID itemId = UUID.randomUUID();
        CartItem item = CartItem.builder()
                .id(itemId)
                .cart(testCart)
                .book(testBook)
                .quantity(2)
                .build();
        testCart.getItems().add(item);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.removeItem("testuser", itemId);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("removeItem - should throw ResourceNotFoundException when item not found")
    void removeItem_NotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

        UUID nonExistentId = UUID.randomUUID();
        assertThatThrownBy(() -> cartService.removeItem("testuser", nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("clearCart - should remove all items")
    void clearCart_Success() {
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID())
                .cart(testCart)
                .book(testBook)
                .quantity(2)
                .build();
        testCart.getItems().add(item);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

        cartService.clearCart("testuser");

        assertThat(testCart.getItems()).isEmpty();
        verify(cartRepository).save(testCart);
    }
}

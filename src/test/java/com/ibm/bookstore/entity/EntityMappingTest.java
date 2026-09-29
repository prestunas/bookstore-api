package com.ibm.bookstore.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMappingTest {

    @Test
    @DisplayName("User entity builder and defaults should initialize correctly")
    void shouldCreateUser() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashed")
                .fullName("Test User")
                .build();

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getRole()).isEqualTo("ROLE_CUSTOMER");
        assertThat(user.getRewardPoints()).isEqualTo(0);
        assertThat(user.getActive()).isTrue();
    }

    @Test
    @DisplayName("Book entity builder and defaults should initialize correctly")
    void shouldCreateBook() {
        UUID id = UUID.randomUUID();
        Category category = Category.builder().id(UUID.randomUUID()).name("Tech").slug("tech").build();
        Author author = Author.builder().id(UUID.randomUUID()).name("Author A").build();
        Publisher publisher = Publisher.builder().id(UUID.randomUUID()).name("Publisher P").build();

        Book book = Book.builder()
                .id(id)
                .title("Sample Book")
                .isbn("1234567890")
                .price(BigDecimal.valueOf(29.99))
                .category(category)
                .author(author)
                .publisher(publisher)
                .build();

        assertThat(book.getId()).isEqualTo(id);
        assertThat(book.getStockQuantity()).isEqualTo(0);
        assertThat(book.getExpectedDeliveryDays()).isEqualTo(3);
        assertThat(book.getActive()).isTrue();
        assertThat(book.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Order and Payment status enum mappings should match OpenAPI specifications")
    void shouldVerifyEnumValues() {
        assertThat(OrderStatus.values()).containsExactlyInAnyOrder(
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAID,
                OrderStatus.CANCELLED,
                OrderStatus.DELIVERED
        );

        assertThat(PaymentStatus.values()).containsExactlyInAnyOrder(
                PaymentStatus.SUCCESS,
                PaymentStatus.FAILED
        );
    }
}

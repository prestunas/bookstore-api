package com.ibm.bookstore.service;

import com.ibm.bookstore.dto.BookSummaryDto;
import com.ibm.bookstore.entity.Author;
import com.ibm.bookstore.entity.Book;
import com.ibm.bookstore.entity.Category;
import com.ibm.bookstore.entity.Publisher;
import com.ibm.bookstore.entity.User;
import com.ibm.bookstore.exception.ResourceNotFoundException;
import com.ibm.bookstore.repository.BookRepository;
import com.ibm.bookstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
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
class RecommendationServiceTest {

    @Mock
    BookRepository bookRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    CatalogService catalogService;

    RecommendationService recommendationService;

    User testUser;
    Book testBook;
    BookSummaryDto testBookSummary;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(bookRepository, userRepository, catalogService);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .build();

        Category category = Category.builder().id(UUID.randomUUID()).name("Tech").build();
        Author author = Author.builder().id(UUID.randomUUID()).name("Author").build();
        Publisher publisher = Publisher.builder().id(UUID.randomUUID()).name("Pub").build();

        testBook = Book.builder()
                .id(UUID.randomUUID())
                .title("Clean Code")
                .isbn("978-0132350884")
                .price(BigDecimal.valueOf(39.99))
                .stockQuantity(10)
                .expectedDeliveryDays(3)
                .active(true)
                .category(category)
                .author(author)
                .publisher(publisher)
                .build();

        testBookSummary = new BookSummaryDto(
                testBook.getId(), "Clean Code", "978-0132350884",
                BigDecimal.valueOf(39.99), 10, null, "Author", "Tech", 3
        );
    }

    @Test
    @DisplayName("getOrderHistoryRecommendations - returns books from order history categories when history exists")
    void getOrderHistoryRecommendations_WithHistory_ReturnsCategoryBooks() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bookRepository.findRecommendationsByOrderHistory(any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of(testBook));
        when(catalogService.toBookSummaryDto(testBook)).thenReturn(testBookSummary);

        List<BookSummaryDto> recommendations = recommendationService.getOrderHistoryRecommendations("testuser", 6);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.getFirst().title()).isEqualTo("Clean Code");
        verify(bookRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getOrderHistoryRecommendations - falls back to latest active books when no order history")
    void getOrderHistoryRecommendations_NoHistory_FallsBackToLatestBooks() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bookRepository.findRecommendationsByOrderHistory(any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testBook)));
        when(catalogService.toBookSummaryDto(testBook)).thenReturn(testBookSummary);

        List<BookSummaryDto> recommendations = recommendationService.getOrderHistoryRecommendations("testuser", 6);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.getFirst().title()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("getOrderHistoryRecommendations - returns empty list when no history and no active books")
    void getOrderHistoryRecommendations_NoHistoryNoBooks_ReturnsEmpty() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bookRepository.findRecommendationsByOrderHistory(any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<BookSummaryDto> recommendations = recommendationService.getOrderHistoryRecommendations("testuser", 6);

        assertThat(recommendations).isEmpty();
    }

    @Test
    @DisplayName("getOrderHistoryRecommendations - throws ResourceNotFoundException when user not found")
    void getOrderHistoryRecommendations_UnknownUser_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.getOrderHistoryRecommendations("unknown", 6))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}

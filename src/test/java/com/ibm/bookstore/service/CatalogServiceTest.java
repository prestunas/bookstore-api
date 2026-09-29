package com.ibm.bookstore.service;

import com.ibm.bookstore.dto.AuthorDto;
import com.ibm.bookstore.dto.BookDetailDto;
import com.ibm.bookstore.dto.BookPageResponse;
import com.ibm.bookstore.dto.BookSummaryDto;
import com.ibm.bookstore.dto.CategoryDto;
import com.ibm.bookstore.dto.PublisherDto;
import com.ibm.bookstore.entity.Author;
import com.ibm.bookstore.entity.Book;
import com.ibm.bookstore.entity.Category;
import com.ibm.bookstore.entity.Publisher;
import com.ibm.bookstore.exception.ResourceNotFoundException;
import com.ibm.bookstore.repository.AuthorRepository;
import com.ibm.bookstore.repository.BookRepository;
import com.ibm.bookstore.repository.CategoryRepository;
import com.ibm.bookstore.repository.PublisherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    CategoryRepository categoryRepository;
    @Mock
    AuthorRepository authorRepository;
    @Mock
    PublisherRepository publisherRepository;
    @Mock
    BookRepository bookRepository;

    CatalogService catalogService;

    Category testCategory;
    Author testAuthor;
    Publisher testPublisher;
    Book testBook;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(categoryRepository, authorRepository, publisherRepository, bookRepository);

        testCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Software Engineering")
                .slug("software-engineering")
                .description("SE books")
                .build();

        testAuthor = Author.builder()
                .id(UUID.randomUUID())
                .name("Robert C. Martin")
                .bio("Author of Clean Code")
                .build();

        testPublisher = Publisher.builder()
                .id(UUID.randomUUID())
                .name("Pearson Education")
                .website("https://pearson.com")
                .build();

        testBook = Book.builder()
                .id(UUID.randomUUID())
                .title("Clean Code")
                .isbn("978-0132350884")
                .description("Code quality guide")
                .price(BigDecimal.valueOf(44.99))
                .stockQuantity(25)
                .coverImageUrl("https://example.com/cover.jpg")
                .expectedDeliveryDays(2)
                .active(true)
                .category(testCategory)
                .author(testAuthor)
                .publisher(testPublisher)
                .build();
    }

    // ---- listCategories ----

    @Test
    @DisplayName("listCategories: should return all categories as DTOs")
    void shouldListAllCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(testCategory));

        List<CategoryDto> result = catalogService.listCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(testCategory.getId());
        assertThat(result.get(0).name()).isEqualTo("Software Engineering");
        assertThat(result.get(0).slug()).isEqualTo("software-engineering");
    }

    @Test
    @DisplayName("listCategories: should return empty list when no categories")
    void shouldReturnEmptyListWhenNoCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<CategoryDto> result = catalogService.listCategories();

        assertThat(result).isEmpty();
    }

    // ---- listAuthors ----

    @Test
    @DisplayName("listAuthors: should return all authors when no query")
    void shouldListAllAuthorsWithoutQuery() {
        when(authorRepository.findAll()).thenReturn(List.of(testAuthor));

        List<AuthorDto> result = catalogService.listAuthors(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Robert C. Martin");
    }

    @Test
    @DisplayName("listAuthors: should filter authors by query")
    void shouldFilterAuthorsWithQuery() {
        when(authorRepository.findByNameContainingIgnoreCase("martin")).thenReturn(List.of(testAuthor));

        List<AuthorDto> result = catalogService.listAuthors("martin");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Robert C. Martin");
    }

    @Test
    @DisplayName("listAuthors: should return all authors for blank query")
    void shouldReturnAllAuthorsForBlankQuery() {
        when(authorRepository.findAll()).thenReturn(List.of(testAuthor));

        List<AuthorDto> result = catalogService.listAuthors("   ");

        assertThat(result).hasSize(1);
    }

    // ---- listPublishers ----

    @Test
    @DisplayName("listPublishers: should return all publishers when no query")
    void shouldListAllPublishersWithoutQuery() {
        when(publisherRepository.findAll()).thenReturn(List.of(testPublisher));

        List<PublisherDto> result = catalogService.listPublishers(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Pearson Education");
        assertThat(result.get(0).website()).isEqualTo("https://pearson.com");
    }

    @Test
    @DisplayName("listPublishers: should filter publishers by query")
    void shouldFilterPublishersWithQuery() {
        when(publisherRepository.findByNameContainingIgnoreCase("pearson")).thenReturn(List.of(testPublisher));

        List<PublisherDto> result = catalogService.listPublishers("pearson");

        assertThat(result).hasSize(1);
    }

    // ---- searchBooks ----

    @Test
    @DisplayName("searchBooks: should return paginated book results")
    void shouldReturnPaginatedBooks() {
        Page<Book> page = new PageImpl<>(List.of(testBook));
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        BookPageResponse result = catalogService.searchBooks(null, null, null, null, 0, 10, "title,asc");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).title()).isEqualTo("Clean Code");
        assertThat(result.content().get(0).isbn()).isEqualTo("978-0132350884");
        assertThat(result.content().get(0).authorName()).isEqualTo("Robert C. Martin");
        assertThat(result.content().get(0).categoryName()).isEqualTo("Software Engineering");
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("searchBooks: should return empty page when no books match")
    void shouldReturnEmptyPageWhenNoBooksMatch() {
        Page<Book> page = new PageImpl<>(List.of());
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        BookPageResponse result = catalogService.searchBooks(UUID.randomUUID(), null, null, "nonexistent", 0, 10, null);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
        assertThat(result.last()).isTrue();
    }

    // ---- getBookById ----

    @Test
    @DisplayName("getBookById: should return book detail DTO for existing book")
    void shouldReturnBookDetailDto() {
        when(bookRepository.findById(testBook.getId())).thenReturn(Optional.of(testBook));

        BookDetailDto result = catalogService.getBookById(testBook.getId());

        assertThat(result.id()).isEqualTo(testBook.getId());
        assertThat(result.title()).isEqualTo("Clean Code");
        assertThat(result.isbn()).isEqualTo("978-0132350884");
        assertThat(result.price()).isEqualByComparingTo(BigDecimal.valueOf(44.99));
        assertThat(result.stockQuantity()).isEqualTo(25);
        assertThat(result.expectedDeliveryDays()).isEqualTo(2);
        assertThat(result.author().name()).isEqualTo("Robert C. Martin");
        assertThat(result.category().name()).isEqualTo("Software Engineering");
        assertThat(result.publisher().name()).isEqualTo("Pearson Education");
    }

    @Test
    @DisplayName("getBookById: should throw ResourceNotFoundException for missing book")
    void shouldThrowWhenBookNotFound() {
        UUID missingId = UUID.randomUUID();
        when(bookRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getBookById(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(missingId.toString());
    }

    // ---- getRelatedBooks ----

    @Test
    @DisplayName("getRelatedBooks: should return books in same category/author excluding target book")
    void shouldReturnRelatedBooks() {
        Book related = Book.builder()
                .id(UUID.randomUUID())
                .title("Clean Architecture")
                .isbn("978-0134494166")
                .price(BigDecimal.valueOf(39.50))
                .stockQuantity(18)
                .expectedDeliveryDays(3)
                .active(true)
                .category(testCategory)
                .author(testAuthor)
                .publisher(testPublisher)
                .build();

        when(bookRepository.findById(testBook.getId())).thenReturn(Optional.of(testBook));
        when(bookRepository.findRelatedBooks(
                eq(testCategory.getId()), eq(testAuthor.getId()), eq(testBook.getId()), any(Pageable.class)
        )).thenReturn(List.of(related));

        List<BookSummaryDto> result = catalogService.getRelatedBooks(testBook.getId(), 4);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Clean Architecture");
    }

    @Test
    @DisplayName("getRelatedBooks: should throw ResourceNotFoundException when target book not found")
    void shouldThrowOnRelatedBooksWhenBookNotFound() {
        UUID missingId = UUID.randomUUID();
        when(bookRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getRelatedBooks(missingId, 4))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- sort parsing edge cases ----

    @Test
    @DisplayName("searchBooks: should handle price,desc sort")
    void shouldHandlePriceDescSort() {
        Page<Book> page = new PageImpl<>(List.of(testBook));
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        BookPageResponse result = catalogService.searchBooks(null, null, null, null, 0, 10, "price,desc");

        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("searchBooks: should fallback to title,asc for unknown sort field")
    void shouldFallbackToTitleAscForUnknownSort() {
        Page<Book> page = new PageImpl<>(List.of(testBook));
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        BookPageResponse result = catalogService.searchBooks(null, null, null, null, 0, 10, "unknown,asc");

        assertThat(result.content()).hasSize(1);
    }
}

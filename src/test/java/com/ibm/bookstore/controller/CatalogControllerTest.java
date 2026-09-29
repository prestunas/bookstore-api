package com.ibm.bookstore.controller;

import com.ibm.bookstore.config.JwtProperties;
import com.ibm.bookstore.config.SecurityConfig;
import com.ibm.bookstore.dto.AuthorDto;
import com.ibm.bookstore.dto.BookDetailDto;
import com.ibm.bookstore.dto.BookPageResponse;
import com.ibm.bookstore.dto.BookSummaryDto;
import com.ibm.bookstore.dto.CategoryDto;
import com.ibm.bookstore.dto.PublisherDto;
import com.ibm.bookstore.exception.GlobalExceptionHandler;
import com.ibm.bookstore.exception.ResourceNotFoundException;
import com.ibm.bookstore.security.JwtAuthenticationFilter;
import com.ibm.bookstore.security.JwtUtils;
import com.ibm.bookstore.service.CatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtils.class, JwtProperties.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "bookstore.security.jwt.secret=test-secret-key-minimum-32-bytes!!",
        "bookstore.security.jwt.expiration-ms=86400000"
})
class CatalogControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CatalogService catalogService;

    static final UUID BOOK_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    static final UUID CATEGORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");
    static final UUID AUTHOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");

    // ---- GET /api/v1/categories ----

    @Test
    @DisplayName("GET /categories: 200 with list of categories")
    void shouldListCategories() throws Exception {
        CategoryDto dto = new CategoryDto(CATEGORY_ID, "Software Engineering", "software-engineering", "SE books");
        when(catalogService.listCategories()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Software Engineering"))
                .andExpect(jsonPath("$[0].slug").value("software-engineering"));
    }

    @Test
    @DisplayName("GET /categories: 200 with empty array when no categories exist")
    void shouldReturnEmptyCategoryList() throws Exception {
        when(catalogService.listCategories()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---- GET /api/v1/authors ----

    @Test
    @DisplayName("GET /authors: 200 with all authors when no query")
    void shouldListAllAuthors() throws Exception {
        AuthorDto dto = new AuthorDto(AUTHOR_ID, "Robert C. Martin", "Author of Clean Code");
        when(catalogService.listAuthors(isNull())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Robert C. Martin"));
    }

    @Test
    @DisplayName("GET /authors?query=martin: 200 with filtered authors")
    void shouldFilterAuthors() throws Exception {
        AuthorDto dto = new AuthorDto(AUTHOR_ID, "Robert C. Martin", "Author of Clean Code");
        when(catalogService.listAuthors("martin")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/authors").param("query", "martin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Robert C. Martin"));
    }

    // ---- GET /api/v1/publishers ----

    @Test
    @DisplayName("GET /publishers: 200 with all publishers")
    void shouldListAllPublishers() throws Exception {
        PublisherDto dto = new PublisherDto(UUID.randomUUID(), "O'Reilly Media", "https://oreilly.com");
        when(catalogService.listPublishers(isNull())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/publishers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("O'Reilly Media"));
    }

    // ---- GET /api/v1/books ----

    @Test
    @DisplayName("GET /books: 200 with paginated book list")
    void shouldListBooks() throws Exception {
        BookSummaryDto summary = new BookSummaryDto(
                BOOK_ID, "Clean Code", "978-0132350884",
                BigDecimal.valueOf(44.99), 25,
                "https://example.com/cover.jpg",
                "Robert C. Martin", "Software Engineering", 2
        );
        BookPageResponse pageResponse = new BookPageResponse(List.of(summary), 0, 10, 1L, 1, true);
        when(catalogService.searchBooks(isNull(), isNull(), isNull(), isNull(), eq(0), eq(10), anyString()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.content[0].authorName").value("Robert C. Martin"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("GET /books?categoryId=...&query=clean: 200 with filtered results")
    void shouldFilterBooks() throws Exception {
        BookSummaryDto summary = new BookSummaryDto(
                BOOK_ID, "Clean Code", "978-0132350884",
                BigDecimal.valueOf(44.99), 25, null,
                "Robert C. Martin", "Software Engineering", 2
        );
        BookPageResponse pageResponse = new BookPageResponse(List.of(summary), 0, 10, 1L, 1, true);
        when(catalogService.searchBooks(eq(CATEGORY_ID), isNull(), isNull(), eq("clean"), eq(0), eq(10), anyString()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/books")
                        .param("categoryId", CATEGORY_ID.toString())
                        .param("query", "clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"));
    }

    // ---- GET /api/v1/books/{id} ----

    @Test
    @DisplayName("GET /books/{id}: 200 with full book details")
    void shouldGetBookById() throws Exception {
        CategoryDto category = new CategoryDto(CATEGORY_ID, "Software Engineering", "software-engineering", null);
        AuthorDto author = new AuthorDto(AUTHOR_ID, "Robert C. Martin", null);
        PublisherDto publisher = new PublisherDto(UUID.randomUUID(), "Pearson", "https://pearson.com");

        BookDetailDto detail = new BookDetailDto(
                BOOK_ID, "Clean Code", "978-0132350884",
                "Code quality guide", BigDecimal.valueOf(44.99), 25,
                "https://example.com/cover.jpg", 2,
                author, category, publisher
        );
        when(catalogService.getBookById(BOOK_ID)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/books/{id}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.isbn").value("978-0132350884"))
                .andExpect(jsonPath("$.expectedDeliveryDays").value(2))
                .andExpect(jsonPath("$.author.name").value("Robert C. Martin"))
                .andExpect(jsonPath("$.category.slug").value("software-engineering"))
                .andExpect(jsonPath("$.publisher.name").value("Pearson"));
    }

    @Test
    @DisplayName("GET /books/{id}: 404 when book not found")
    void shouldReturn404WhenBookNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(catalogService.getBookById(missingId))
                .thenThrow(new ResourceNotFoundException("Book", missingId));

        mockMvc.perform(get("/api/v1/books/{id}", missingId))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/v1/books/{id}/related ----

    @Test
    @DisplayName("GET /books/{id}/related: 200 with related books")
    void shouldGetRelatedBooks() throws Exception {
        BookSummaryDto related = new BookSummaryDto(
                UUID.randomUUID(), "Clean Architecture", "978-0134494166",
                BigDecimal.valueOf(39.50), 18, null,
                "Robert C. Martin", "Software Engineering", 3
        );
        when(catalogService.getRelatedBooks(eq(BOOK_ID), anyInt())).thenReturn(List.of(related));

        mockMvc.perform(get("/api/v1/books/{id}/related", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Clean Architecture"));
    }

    @Test
    @DisplayName("GET /books/{id}/related: 404 when source book not found")
    void shouldReturn404OnRelatedBooksWhenBookNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(catalogService.getRelatedBooks(eq(missingId), anyInt()))
                .thenThrow(new ResourceNotFoundException("Book", missingId));

        mockMvc.perform(get("/api/v1/books/{id}/related", missingId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /books/{id}/related: 200 with custom limit param")
    void shouldRespectLimitParam() throws Exception {
        when(catalogService.getRelatedBooks(eq(BOOK_ID), eq(8))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/books/{id}/related", BOOK_ID).param("limit", "8"))
                .andExpect(status().isOk());
    }
}

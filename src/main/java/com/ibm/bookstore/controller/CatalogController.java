package com.ibm.bookstore.controller;

import com.ibm.bookstore.dto.AuthorDto;
import com.ibm.bookstore.dto.BookDetailDto;
import com.ibm.bookstore.dto.BookPageResponse;
import com.ibm.bookstore.dto.BookSummaryDto;
import com.ibm.bookstore.dto.CategoryDto;
import com.ibm.bookstore.dto.PublisherDto;
import com.ibm.bookstore.service.CatalogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CatalogController {

    CatalogService catalogService;

    @GetMapping("/categories")
    public List<CategoryDto> listCategories() {
        return catalogService.listCategories();
    }

    @GetMapping("/authors")
    public List<AuthorDto> listAuthors(
            @RequestParam(required = false) String query
    ) {
        return catalogService.listAuthors(query);
    }

    @GetMapping("/publishers")
    public List<PublisherDto> listPublishers(
            @RequestParam(required = false) String query
    ) {
        return catalogService.listPublishers(query);
    }

    @GetMapping("/books")
    public BookPageResponse listBooks(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) UUID publisherId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title,asc") String sort
    ) {
        return catalogService.searchBooks(categoryId, authorId, publisherId, query, page, size, sort);
    }

    @GetMapping("/books/{id}")
    public BookDetailDto getBookById(@PathVariable UUID id) {
        return catalogService.getBookById(id);
    }

    @GetMapping("/books/{id}/related")
    public List<BookSummaryDto> getRelatedBooks(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "4") int limit
    ) {
        return catalogService.getRelatedBooks(id, limit);
    }
}

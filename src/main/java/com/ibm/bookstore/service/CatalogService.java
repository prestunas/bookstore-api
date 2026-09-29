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
import com.ibm.bookstore.repository.BookSpecification;
import com.ibm.bookstore.repository.CategoryRepository;
import com.ibm.bookstore.repository.PublisherRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CatalogService {

    CategoryRepository categoryRepository;
    AuthorRepository authorRepository;
    PublisherRepository publisherRepository;
    BookRepository bookRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> listCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorDto> listAuthors(String query) {
        List<Author> authors = (query == null || query.isBlank())
                ? authorRepository.findAll()
                : authorRepository.findByNameContainingIgnoreCase(query);
        return authors.stream()
                .map(this::toAuthorDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublisherDto> listPublishers(String query) {
        List<Publisher> publishers = (query == null || query.isBlank())
                ? publisherRepository.findAll()
                : publisherRepository.findByNameContainingIgnoreCase(query);
        return publishers.stream()
                .map(this::toPublisherDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookPageResponse searchBooks(UUID categoryId, UUID authorId, UUID publisherId,
                                        String query, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<Book> bookPage = bookRepository.findAll(
                BookSpecification.buildFilter(categoryId, authorId, publisherId, query),
                pageable
        );
        List<BookSummaryDto> content = bookPage.getContent().stream()
                .map(this::toBookSummaryDto)
                .toList();
        return new BookPageResponse(
                content,
                bookPage.getNumber(),
                bookPage.getSize(),
                bookPage.getTotalElements(),
                bookPage.getTotalPages(),
                bookPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public BookDetailDto getBookById(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
        return toBookDetailDto(book);
    }

    @Transactional(readOnly = true)
    public List<BookSummaryDto> getRelatedBooks(UUID id, int limit) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findRelatedBooks(
                        book.getCategory().getId(),
                        book.getAuthor().getId(),
                        id,
                        pageable
                ).stream()
                .map(this::toBookSummaryDto)
                .toList();
    }

    // ---- Mappers ----

    private CategoryDto toCategoryDto(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.getDescription());
    }

    private AuthorDto toAuthorDto(Author a) {
        return new AuthorDto(a.getId(), a.getName(), a.getBio());
    }

    private PublisherDto toPublisherDto(Publisher p) {
        return new PublisherDto(p.getId(), p.getName(), p.getWebsite());
    }

    BookSummaryDto toBookSummaryDto(Book b) {
        return new BookSummaryDto(
                b.getId(),
                b.getTitle(),
                b.getIsbn(),
                b.getPrice(),
                b.getStockQuantity(),
                b.getCoverImageUrl(),
                b.getAuthor().getName(),
                b.getCategory().getName(),
                b.getExpectedDeliveryDays()
        );
    }

    private BookDetailDto toBookDetailDto(Book b) {
        return new BookDetailDto(
                b.getId(),
                b.getTitle(),
                b.getIsbn(),
                b.getDescription(),
                b.getPrice(),
                b.getStockQuantity(),
                b.getCoverImageUrl(),
                b.getExpectedDeliveryDays(),
                toAuthorDto(b.getAuthor()),
                toCategoryDto(b.getCategory()),
                toPublisherDto(b.getPublisher())
        );
    }

    /**
     * Parses a sort string in the format "field,direction" (e.g. "price,asc" or "title,desc").
     * Defaults to title,asc if the string is blank or cannot be parsed.
     * Only allows known sortable fields to prevent arbitrary column injection.
     */
    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "title");
        }
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return switch (field) {
            case "price" -> Sort.by(direction, "price");
            case "title" -> Sort.by(direction, "title");
            case "createdAt" -> Sort.by(direction, "createdAt");
            default -> Sort.by(Sort.Direction.ASC, "title");
        };
    }
}

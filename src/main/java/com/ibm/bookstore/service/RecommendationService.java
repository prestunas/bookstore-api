package com.ibm.bookstore.service;

import com.ibm.bookstore.dto.BookSummaryDto;
import com.ibm.bookstore.entity.Book;
import com.ibm.bookstore.entity.User;
import com.ibm.bookstore.exception.ResourceNotFoundException;
import com.ibm.bookstore.repository.BookRepository;
import com.ibm.bookstore.repository.BookSpecification;
import com.ibm.bookstore.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RecommendationService {

    BookRepository bookRepository;
    UserRepository userRepository;
    CatalogService catalogService;

    @Transactional(readOnly = true)
    public List<BookSummaryDto> getOrderHistoryRecommendations(String username, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        List<Book> books = bookRepository.findRecommendationsByOrderHistory(
                user.getId(),
                PageRequest.of(0, limit)
        );
        if (books.isEmpty()) {
            // Fall back to most recently added active books when no order history exists
            books = bookRepository.findAll(
                    BookSpecification.activeOnly(),
                    PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).getContent();
        }
        return books.stream()
                .map(catalogService::toBookSummaryDto)
                .toList();
    }
}

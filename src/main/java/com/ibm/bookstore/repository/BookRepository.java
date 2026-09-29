package com.ibm.bookstore.repository;

import com.ibm.bookstore.entity.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByIsbn(String isbn);

    @Query("SELECT b FROM Book b WHERE b.active = true AND (b.category.id = :categoryId OR b.author.id = :authorId) AND b.id <> :excludeId")
    List<Book> findRelatedBooks(@Param("categoryId") UUID categoryId,
                                @Param("authorId") UUID authorId,
                                @Param("excludeId") UUID excludeId,
                                Pageable pageable);

    @Query("SELECT DISTINCT b FROM OrderItem oi JOIN oi.book b JOIN oi.order o WHERE o.user.id = :userId AND o.status = 'PAID' ORDER BY b.title ASC")
    List<Book> findBuyAgainBooksByUser(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT b FROM Book b WHERE b.active = true AND b.category.id IN (" +
            "SELECT DISTINCT oi.book.category.id FROM OrderItem oi WHERE oi.order.user.id = :userId AND oi.order.status = 'PAID'" +
            ") AND b.id NOT IN (" +
            "SELECT DISTINCT oi.book.id FROM OrderItem oi WHERE oi.order.user.id = :userId" +
            ")")
    List<Book> findRecommendationsByOrderHistory(@Param("userId") UUID userId, Pageable pageable);
}

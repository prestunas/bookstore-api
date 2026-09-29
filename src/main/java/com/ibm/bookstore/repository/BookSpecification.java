package com.ibm.bookstore.repository;

import com.ibm.bookstore.entity.Book;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BookSpecification {

    private BookSpecification() {
    }

    public static Specification<Book> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Book> withCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Book> withAuthor(UUID authorId) {
        if (authorId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("author").get("id"), authorId);
    }

    public static Specification<Book> withPublisher(UUID publisherId) {
        if (publisherId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("publisher").get("id"), publisherId);
    }

    public static Specification<Book> withKeyword(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String pattern = "%" + query.toLowerCase() + "%";
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("title")), pattern));
            predicates.add(cb.like(cb.lower(root.get("description")), pattern));
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Book> buildFilter(UUID categoryId, UUID authorId, UUID publisherId, String query) {
        Specification<Book> spec = activeOnly();
        Specification<Book> category = withCategory(categoryId);
        if (category != null) {
            spec = spec.and(category);
        }
        Specification<Book> author = withAuthor(authorId);
        if (author != null) {
            spec = spec.and(author);
        }
        Specification<Book> publisher = withPublisher(publisherId);
        if (publisher != null) {
            spec = spec.and(publisher);
        }
        Specification<Book> keyword = withKeyword(query);
        if (keyword != null) {
            spec = spec.and(keyword);
        }
        return spec;
    }
}

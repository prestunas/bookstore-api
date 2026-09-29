package com.ibm.bookstore.repository;

import com.ibm.bookstore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByCartIdAndBookId(UUID cartId, UUID bookId);

    void deleteByCartId(UUID cartId);
}

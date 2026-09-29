package com.ibm.bookstore.repository;

import com.ibm.bookstore.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PublisherRepository extends JpaRepository<Publisher, UUID> {

    List<Publisher> findByNameContainingIgnoreCase(String name);
}

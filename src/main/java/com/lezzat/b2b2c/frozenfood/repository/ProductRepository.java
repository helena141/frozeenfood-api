package com.lezzat.b2b2c.frozenfood.repository;

import com.lezzat.b2b2c.frozenfood.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID; // <-- Tambahan import UUID

// Anotasi @Repository dihapus
public interface ProductRepository extends JpaRepository<Product, UUID> { // <-- Ganti Long jadi UUID
}
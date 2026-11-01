package com.lezzat.b2b2c.frozenfood.repository;

import com.lezzat.b2b2c.frozenfood.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    // Fitur Search: Mencari produk berdasarkan nama (mengabaikan huruf besar/kecil)
    List<Product> findByNamaProdukContainingIgnoreCase(String namaProduk, String kategori);
    
}
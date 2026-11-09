package com.lezzat.b2b2c.frozenfood.repository;

import com.lezzat.b2b2c.frozenfood.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional; // Tambahkan import ini untuk membungkus hasil pencarian

// === 1. Ubah UUID menjadi String di sini ===
public interface ProductRepository extends JpaRepository<Product, String> {
    
    // Mencari produk yang namanya cocok ATAU kategorinya cocok
    List<Product> findByNamaProdukContainingIgnoreCaseOrKategoriContainingIgnoreCase(String namaProduk, String kategori);
    
    // === MENCEGAH DUPLIKAT NAMA PRODUK ===
    boolean existsByNamaProdukIgnoreCase(String namaProduk);
    
    // === 2. TAMBAHKAN BARIS INI UNTUK MENCARI KODE PRODUK TERAKHIR ===
    Optional<Product> findTopByIdProdukStartingWithOrderByIdProdukDesc(String prefix);
    
}
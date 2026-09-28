package com.lezzat.b2b2c.frozenfood.repository;

import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, UUID> {
    
    // Kata "Product_IdProduk" akan menyuruh Spring mencari relasi ke tabel Product, 
    // lalu mencari variabel bernama "idProduk" di dalamnya. Ini akan menghilangkan error 100%!
    List<ProductBatch> findByProduct_IdProdukAndStokOnlineGreaterThanOrderByTanggalExpAsc(UUID idProduk, Integer stokOnline);
    
}
package com.lezzat.b2b2c.frozenfood.repository;

import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductBatchRepository extends JpaRepository<ProductBatch, UUID> {
    
    /**
     * INTI DARI FEFO:
     * Mencari batch milik suatu produk yang stok onlinenya lebih dari 0,
     * lalu diurutkan dari tanggal kedaluwarsa (tanggal_exp) paling awal (Ascending).
     */
    List<ProductBatch> findByProductIdAndStokOnlineGreaterThanOrderByTanggalExpAsc(UUID productId, Integer stokOnline);
}
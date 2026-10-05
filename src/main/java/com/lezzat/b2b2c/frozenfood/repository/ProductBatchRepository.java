package com.lezzat.b2b2c.frozenfood.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;

public interface ProductBatchRepository extends JpaRepository<ProductBatch, UUID> {

    // 1. MESIN UTAMA FEFO (First Expired, First Out)
    @Query("SELECT pb FROM ProductBatch pb WHERE pb.product.idProduk = :productId AND pb.stokOnline > 0 ORDER BY pb.tanggalExp ASC")
    List<ProductBatch> findAvailableBatchesForFefo(@Param("productId") UUID productId);

    // 2. RADAR EXPIRED (Untuk Cron Job ExpiryAlertService) - ANTI N+1 PROBLEM
    @Query("SELECT pb FROM ProductBatch pb JOIN FETCH pb.product WHERE pb.tanggalExp <= :targetDate AND pb.stokOnline > 0 ORDER BY pb.tanggalExp ASC")
    List<ProductBatch> findBatchesExpiringBefore(@Param("targetDate") LocalDate targetDate);
}
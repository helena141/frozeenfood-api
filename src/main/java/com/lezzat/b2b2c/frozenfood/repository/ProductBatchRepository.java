package com.lezzat.b2b2c.frozenfood.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, UUID> {

    // 1. KODE BONY: FEFO Inventory Service (Parameter diselaraskan menjadi productId)
    List<ProductBatch> findByProduct_IdProdukAndStokOnlineGreaterThanOrderByTanggalExpAsc(UUID productId, int minStok);

    // 2. KODE MORENO: Mesin ExpiryAlert (Anti N+1 Problem)
    @Query("SELECT pb FROM ProductBatch pb JOIN FETCH pb.product WHERE pb.tanggalExp <= :targetDate AND pb.stokOnline > 0 ORDER BY pb.tanggalExp ASC")
    List<ProductBatch> findBatchesExpiringBefore(@Param("targetDate") LocalDate targetDate);
}
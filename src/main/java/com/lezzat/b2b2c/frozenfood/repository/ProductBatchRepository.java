package com.lezzat.b2b2c.frozenfood.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;

public interface ProductBatchRepository extends JpaRepository<ProductBatch, String> {

    // 1. KODE BONY: FEFO Inventory Service (Parameter diselaraskan menjadi productId)
    @Query("SELECT pb FROM ProductBatch pb JOIN FETCH pb.product WHERE pb.product.idProduk = :productId AND pb.stokOnline > :minStok ORDER BY pb.tanggalExp ASC")
    List<ProductBatch> findByProduct_IdProdukAndStokOnlineGreaterThanOrderByTanggalExpAsc(@Param("productId") String productId, @Param("minStok") int minStok);

    @Query("SELECT pb FROM ProductBatch pb JOIN FETCH pb.product WHERE pb.tanggalExp <= :targetDate AND pb.stokOnline > 0 ORDER BY pb.tanggalExp ASC")
    List<ProductBatch> findBatchesExpiringBefore(@Param("targetDate") LocalDate targetDate);

    @Query("SELECT pb FROM ProductBatch pb JOIN FETCH pb.product ORDER BY pb.tanggalExp ASC")
    List<ProductBatch> findAllWithProductOrderByTanggalExpAsc();

    @Query("SELECT pb FROM ProductBatch pb JOIN FETCH pb.product WHERE pb.product.idProduk = :productId ORDER BY pb.tanggalExp ASC")
    List<ProductBatch> findByProductIdWithProductOrderByTanggalExpAsc(@Param("productId") String productId);
}
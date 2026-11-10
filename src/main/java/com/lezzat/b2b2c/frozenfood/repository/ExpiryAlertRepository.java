package com.lezzat.b2b2c.frozenfood.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.lezzat.b2b2c.frozenfood.entity.ExpiryAlert;
import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;

@Repository
public interface ExpiryAlertRepository extends JpaRepository<ExpiryAlert, UUID> {
    // Cek apakah batch ini sudah punya alert yang belum diselesaikan
    boolean existsByProductBatchAndIsResolvedFalse(ProductBatch productBatch);

    @Query("SELECT a FROM ExpiryAlert a JOIN FETCH a.productBatch pb JOIN FETCH pb.product ORDER BY a.createdAt DESC")
    List<ExpiryAlert> findAllWithBatchAndProduct();
}
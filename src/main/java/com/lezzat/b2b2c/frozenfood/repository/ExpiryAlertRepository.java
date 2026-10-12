package com.lezzat.b2b2c.frozenfood.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lezzat.b2b2c.frozenfood.entity.ExpiryAlert;
import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;

@Repository
public interface ExpiryAlertRepository extends JpaRepository<ExpiryAlert, UUID> {
    // Cek apakah batch ini sudah punya alert yang belum diselesaikan
    boolean existsByProductBatchAndIsResolvedFalse(ProductBatch productBatch);
}
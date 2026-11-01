package com.lezzat.b2b2c.frozenfood.repository;

import com.lezzat.b2b2c.frozenfood.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Repository baru — belum ada yang membuat ini sebelumnya (OrderItem adalah
// sub-entity dari Order, tapi query untuk laporan ini spesifik kebutuhan
// Report, jadi dibuat di sini). JOIN FETCH ke order & productBatch.product
// supaya tidak kena N+1 / LazyInitializationException saat dipakai di
// ReportService.
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("SELECT oi FROM OrderItem oi " +
            "JOIN FETCH oi.order o " +
            "JOIN FETCH oi.productBatch pb " +
            "JOIN FETCH pb.product p " +
            "WHERE o.createdAt BETWEEN :awal AND :akhir " +
            "AND o.status <> 'DIBATALKAN'")
    List<OrderItem> findForLaporanPenjualan(@Param("awal") LocalDateTime awal,
                                             @Param("akhir") LocalDateTime akhir);
}

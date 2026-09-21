package com.lezzat.b2b2c.frozenfood.repository;

import com.lezzat.b2b2c.frozenfood.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByOrder_IdOrder(UUID idOrder);
    Optional<Payment> findByGatewayRef(String gatewayRef);

    // Dipakai ReportService untuk hitung omzet: pembayaran LUNAS dalam rentang waktu tertentu.
    List<Payment> findByStatusAndPaidAtBetween(String status, LocalDateTime awal, LocalDateTime akhir);
}

package com.lezzat.b2b2c.frozenfood.repository;

import com.lezzat.b2b2c.frozenfood.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    // Pencarian pembayaran berdasarkan ID struk pesanan utama
    Optional<Payment> findByOrder_IdOrder(UUID idOrder);
    
    // Sangat krusial untuk fitur callback dari Payment Gateway (misal: Midtrans/Xendit)
    Optional<Payment> findByGatewayRef(String gatewayRef);
}
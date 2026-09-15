package com.lezzat.b2b2c.frozenfood.repository;

import com.lezzat.b2b2c.frozenfood.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    
    // Mencari rincian pengiriman berdasarkan pesanan
    Optional<Delivery> findByOrder_IdOrder(UUID idOrder);
}
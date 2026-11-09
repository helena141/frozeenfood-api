package com.lezzat.b2b2c.frozenfood.repository;

import com.lezzat.b2b2c.frozenfood.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByOrder_IdOrder(UUID idOrder);

    /** Dipakai planner untuk tahu berapa pengantar sedang bertugas. */
    long countByPelaksanaAndStatusDeliveryIn(String pelaksana, Collection<String> statusDelivery);
}

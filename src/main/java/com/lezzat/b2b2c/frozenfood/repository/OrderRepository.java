package com.lezzat.b2b2c.frozenfood.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lezzat.b2b2c.frozenfood.entity.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {

// List<Order> findByCustomerId(UUID customerId);


    List<Order> findByCustomerIdUser(UUID idUser);


    // JOIN FETCH untuk menghindari N+1 Query saat membaca detail barang
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.idOrder = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") UUID orderId);
}

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
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.productBatch pb LEFT JOIN FETCH pb.product WHERE o.idOrder = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") UUID orderId);

    // Ambil daftar order milik customer lengkap dengan rincian item & produknya (urut terbaru)
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.productBatch pb LEFT JOIN FETCH pb.product WHERE o.customer.idUser = :idUser ORDER BY o.createdAt DESC")
    List<Order> findByCustomerIdUserWithItems(@Param("idUser") UUID idUser);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.productBatch pb LEFT JOIN FETCH pb.product ORDER BY o.createdAt DESC")
    List<Order> findAllWithItems();
}

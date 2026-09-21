package com.lezzat.b2b2c.frozenfood.repository;

// SEMENTARA — file ini bukan tanggung jawab Krisna, melainkan Moreno
// (modul core-b2b, pemegang penuh Order/TieringPrice/ExpiryAlert/Auth).
// Dibuat minimal di sini cuma supaya DeliveryService & PaymentService bisa
// dicompile dan dites sendiri lebih dulu. GANTI/gabungkan dengan
// OrderRepository asli buatan Moreno sebelum merge — terutama kalau dia
// sudah menambahkan custom query method lain di situ.

import com.lezzat.b2b2c.frozenfood.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}

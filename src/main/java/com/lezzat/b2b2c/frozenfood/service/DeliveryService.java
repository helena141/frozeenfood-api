package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.dto.request.DeliveryRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.DeliveryResponse;
import com.lezzat.b2b2c.frozenfood.entity.Delivery;
import com.lezzat.b2b2c.frozenfood.entity.Order;
import com.lezzat.b2b2c.frozenfood.repository.DeliveryRepository;
import com.lezzat.b2b2c.frozenfood.repository.OrderRepository;
import com.lezzat.b2b2c.frozenfood.service.core.SmartDeliveryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ASUMSI: OrderRepository di sini milik modul Moreno (core-b2b).
 * DeliveryService hanya BACA data Order untuk validasi id_order, tidak pernah
 * membuat/mengubah/ menghapus Order.
 *
 * CATATAN: entity Delivery tidak menandai createdAt/updatedAt sebagai
 * insertable=false/updatable=false (beda dari entity lain seperti Order), jadi
 * di sini diisi manual supaya tidak NULL saat disimpan.
 */
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private static final String STATUS_MENUNGGU_PICKUP = "MENUNGGU_PICKUP";

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final SmartDeliveryService smartDeliveryService;

    @Transactional
    public DeliveryResponse createDelivery(DeliveryRequest request) {
        // Validasi radius aman DULU. Kalau di luar radius, method ini melempar
        // DeliveryRadiusExceededException dan proses berhenti di sini.
        smartDeliveryService.validasiRadiusAman(request.latitudeTujuan(), request.longitudeTujuan());
        BigDecimal jarakKm = smartDeliveryService.hitungJarakKm(
                request.latitudeTujuan(), request.longitudeTujuan());

        Order order = orderRepository.findById(request.idOrder())
                .orElseThrow(() -> new EntityNotFoundException(
                "Order dengan id " + request.idOrder() + " tidak ditemukan"));

        LocalDateTime now = LocalDateTime.now();
        Delivery delivery = Delivery.builder()
                .order(order)
                .alamatTujuan(request.alamatTujuan())
                .jarak(jarakKm.floatValue())
                .metodeDeliv(request.metodeDeliv())
                .statusDelivery(STATUS_MENUNGGU_PICKUP)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Delivery saved = deliveryRepository.save(delivery);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getById(UUID idDelivery) {
        return toResponse(findOrThrow(idDelivery));
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getByOrderId(UUID idOrder) {
        Delivery delivery = deliveryRepository.findByOrder_IdOrder(idOrder)
                .orElseThrow(() -> new EntityNotFoundException(
                "Delivery untuk order " + idOrder + " tidak ditemukan"));
        return toResponse(delivery);
    }

    @Transactional
    public DeliveryResponse updateStatus(UUID idDelivery, String statusDeliveryBaru) {
        Delivery delivery = findOrThrow(idDelivery);
        delivery.setStatusDelivery(statusDeliveryBaru);
        delivery.setUpdatedAt(LocalDateTime.now());
        return toResponse(delivery);
    }

    private Delivery findOrThrow(UUID idDelivery) {
        return deliveryRepository.findById(idDelivery)
                .orElseThrow(() -> new EntityNotFoundException(
                "Delivery dengan id " + idDelivery + " tidak ditemukan"));
    }

    private DeliveryResponse toResponse(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getIdDelivery(),
                delivery.getOrder().getIdOrder(),
                delivery.getAlamatTujuan(),
                delivery.getJarak(),
                delivery.getMetodeDeliv(),
                delivery.getStatusDelivery(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }
}

package com.lezzat.b2b2c.frozenfood.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lezzat.b2b2c.frozenfood.dto.request.PaymentCallbackRequest;
import com.lezzat.b2b2c.frozenfood.dto.request.PaymentRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.PaymentResponse;
import com.lezzat.b2b2c.frozenfood.entity.Order;
import com.lezzat.b2b2c.frozenfood.entity.Payment;
import com.lezzat.b2b2c.frozenfood.repository.OrderRepository;
import com.lezzat.b2b2c.frozenfood.repository.PaymentRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * ASUMSI: OrderRepository di sini milik modul Moreno (core-b2b). PaymentService
 * hanya BACA data Order (ambil totalTagihan untuk "jumlah"), tidak pernah
 * membuat/mengubah/menghapus Order.
 *
 * CATATAN: entity Payment tidak menandai createdAt/updatedAt sebagai
 * insertable=false/updatable=false, jadi diisi manual di sini supaya tidak NULL
 * saat disimpan.
 *
 * TODO (belum diimplementasikan): integrasi payment gateway asli (mis.
 * Midtrans/Xendit). Saat ini gatewayRef di-generate sendiri sebagai placeholder
 * ("MOCK-..."), belum benar-benar memanggil API gateway manapun. Signature
 * method sengaja dibiarkan sama supaya PaymentController tidak perlu ikut
 * berubah saat integrasi asli dipasang nanti.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_LUNAS = "LUNAS";

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    // Masa berlaku kode pembayaran (VA/QRIS) sebelum expiredAt — belum ada
    // keputusan bisnis resmi, default 24 jam, configurable lewat
    // application.properties: lezzat.payment.expiry-hours
    @Value("${lezzat.payment.expiry-hours:24}")
    private long paymentExpiryHours;

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.idOrder())
                .orElseThrow(() -> new EntityNotFoundException(
                "Order dengan id " + request.idOrder() + " tidak ditemukan"));

        LocalDateTime now = LocalDateTime.now();
        Payment payment = Payment.builder()
                .order(order)
                .gatewayRef("MOCK-" + UUID.randomUUID())
                .metodePay(request.metodePay())
                .jumlah(order.getTotalTagihan())
                .status(STATUS_PENDING)
                .expiredAt(now.plusHours(paymentExpiryHours))
                .createdAt(now)
                .updatedAt(now)
                .build();

        Payment saved = paymentRepository.save(payment);
        return toResponse(saved);
    }

    /**
     * Dipanggil dari endpoint webhook saat payment gateway mengonfirmasi status
     * pembayaran. Kalau status yang dikirim gateway menandakan sukses (LUNAS),
     * paidAt diisi sekarang.
     */
    @Transactional
    public PaymentResponse handleCallback(PaymentCallbackRequest request) {
        Payment payment = paymentRepository.findByGatewayRef(request.gatewayRef())
                .orElseThrow(() -> new EntityNotFoundException(
                "Payment dengan gatewayRef " + request.gatewayRef() + " tidak ditemukan"));

        payment.setStatus(request.status());
        if (STATUS_LUNAS.equalsIgnoreCase(request.status())) {
            payment.setPaidAt(LocalDateTime.now());
        }
        payment.setUpdatedAt(LocalDateTime.now());

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(UUID idPayment) {
        return toResponse(findOrThrow(idPayment));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getByOrderId(UUID idOrder) {
        return paymentRepository.findByOrder_IdOrder(idOrder)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Payment findOrThrow(UUID idPayment) {
        return paymentRepository.findById(idPayment)
                .orElseThrow(() -> new EntityNotFoundException(
                "Payment dengan id " + idPayment + " tidak ditemukan"));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getIdPayment(),
                payment.getOrder().getIdOrder(),
                payment.getGatewayRef(),
                payment.getMetodePay(),
                payment.getJumlah(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getExpiredAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}

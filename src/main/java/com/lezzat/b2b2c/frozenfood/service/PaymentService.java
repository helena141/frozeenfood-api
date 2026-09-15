package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.dto.request.PaymentCallbackRequest;
import com.lezzat.b2b2c.frozenfood.entity.Payment;
import com.lezzat.b2b2c.frozenfood.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public void processPaymentCallback(PaymentCallbackRequest request) {
        // 1. Cari data pembayaran berdasarkan referensi gateway
        Payment payment = paymentRepository.findByGatewayRef(request.gatewayRef())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Penolakan Sistem: Payment dengan Gateway Ref " + request.gatewayRef() + " tidak ditemukan."));

        // 2. Validasi Keamanan Finansial (Pastikan nominal tidak dimanipulasi)
        if (payment.getJumlah().compareTo(request.jumlah()) != 0) {
            throw new IllegalArgumentException(
                    "Penolakan Sistem: Nominal pembayaran tidak sesuai dengan tagihan.");
        }

        // 3. Update status pembayaran dan status pesanan utama
        if ("SUCCESS".equalsIgnoreCase(request.status())) {
            payment.setStatus("PAID");
            payment.setPaidAt(LocalDateTime.now());
            // Karena relasi bersifat Lazy, kita bisa langsung update entity induk
            payment.getOrder().setStatus("DIBAYAR"); 
        } else if ("FAILED".equalsIgnoreCase(request.status()) || "EXPIRED".equalsIgnoreCase(request.status())) {
            payment.setStatus(request.status().toUpperCase());
            payment.getOrder().setStatus("GAGAL_BAYAR");
        }
        
        // 4. Simpan perubahan ke database
        paymentRepository.save(payment);
    }
}
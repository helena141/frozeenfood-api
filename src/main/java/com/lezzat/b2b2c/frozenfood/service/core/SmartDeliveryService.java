package com.lezzat.b2b2c.frozenfood.service.core;

import org.springframework.stereotype.Service;

@Service
public class SmartDeliveryService {

    // Batas aman pengiriman Frozen Food agar tidak mencair (misal: 25 KM)
    private static final Float MAX_SAFE_RADIUS_KM = 25.0f;

    /**
     * Memvalidasi apakah jarak pengiriman instan (B2C) aman.
     * Akan dipanggil oleh OrderService atau DeliveryService sebelum checkout berhasil.
     */
    public void validateB2CDeliveryRadius(Float distanceKm) {
        if (distanceKm == null || distanceKm <= 0) {
            throw new IllegalArgumentException("Jarak tempuh pengiriman tidak valid.");
        }
        
        if (distanceKm > MAX_SAFE_RADIUS_KM) {
            throw new IllegalStateException(
                String.format("Penolakan Sistem: Jarak %s km melebihi batas aman radius instan (%s km). Risiko tinggi produk mencair di jalan.", 
                distanceKm, MAX_SAFE_RADIUS_KM)
            );
        }
    }
}
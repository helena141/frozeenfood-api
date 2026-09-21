package com.lezzat.b2b2c.frozenfood.service.core;

import com.lezzat.b2b2c.frozenfood.exception.DeliveryRadiusExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Algoritma "Smart Delivery Radius" (B2C).
 * Menghitung jarak garis lurus (Haversine) antara gudang dan alamat tujuan,
 * lalu memvalidasi apakah masih dalam radius aman supaya produk frozen food
 * tidak mencair selama pengiriman instan.
 *
 * NILAI DI BAWAH INI MASIH ASUMSI / PLACEHOLDER — belum ada keputusan bisnis
 * resmi soal radius aman maupun koordinat gudang yang sebenarnya. Sengaja
 * dibuat configurable lewat application.properties/application.yml supaya
 * bisa diubah tanpa compile ulang:
 *
 *   lezzat.warehouse.latitude=-7.797068
 *   lezzat.warehouse.longitude=110.370529
 *   lezzat.delivery.max-radius-km=15.0
 */
@Service
public class SmartDeliveryService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Value("${lezzat.warehouse.latitude:-7.797068}")
    private double warehouseLatitude;

    @Value("${lezzat.warehouse.longitude:110.370529}")
    private double warehouseLongitude;

    @Value("${lezzat.delivery.max-radius-km:15.0}")
    private double maxRadiusKm;

    /**
     * Menghitung jarak (km) dari gudang ke koordinat tujuan.
     */
    public BigDecimal hitungJarakKm(double destLatitude, double destLongitude) {
        double distance = haversine(warehouseLatitude, warehouseLongitude, destLatitude, destLongitude);
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Melempar DeliveryRadiusExceededException kalau tujuan di luar radius aman.
     * Dipanggil DeliveryService SEBELUM data Delivery disimpan.
     */
    public void validasiRadiusAman(double destLatitude, double destLongitude) {
        BigDecimal jarak = hitungJarakKm(destLatitude, destLongitude);
        BigDecimal batasAman = BigDecimal.valueOf(maxRadiusKm);

        if (jarak.compareTo(batasAman) > 0) {
            throw new DeliveryRadiusExceededException(
                    "Jarak pengiriman " + jarak + " km melebihi radius aman "
                            + batasAman + " km. Produk frozen food berisiko mencair."
            );
        }
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}

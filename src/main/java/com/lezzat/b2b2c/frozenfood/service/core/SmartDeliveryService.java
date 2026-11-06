package com.lezzat.b2b2c.frozenfood.service.core;

import com.lezzat.b2b2c.frozenfood.exception.DeliveryRadiusExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Algoritma "Smart Delivery Radius" (B2C).
 *
 * Perhitungan jarak SAAT INI pakai Haversine (jarak garis lurus) dikali
 * faktor koreksi jalan, sebagai alternatif sementara karena integrasi
 * Google Routes API (jarak jalan asli) butuh API key + billing yang belum
 * tersedia. Semua nilai di bawah configurable lewat
 * application.properties/application.yml supaya gampang diubah nanti:
 *
 *   lezzat.warehouse.latitude=-7.756318
 *   lezzat.warehouse.longitude=110.371033
 *   lezzat.delivery.max-radius-km=15.0
 *   lezzat.delivery.road-distance-factor=1.3
 *
 * TODO (upgrade path): kalau budget Google Routes API sudah tersedia,
 * ganti isi method hitungJarakKm() untuk panggil endpoint
 * computeRouteMatrix milik Routes API (bukan Distance Matrix API lama yang
 * sudah legacy sejak Maret 2025), dan hapus logic faktor pengali ini.
 * Signature method (hitungJarakKm, validasiRadiusAman) sengaja dibiarkan
 * sama supaya DeliveryService tidak perlu ikut berubah saat upgrade nanti.
 */
@Service
public class SmartDeliveryService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final RouteDistanceClient routeDistanceClient;

    public SmartDeliveryService(RouteDistanceClient routeDistanceClient) {
        this.routeDistanceClient = routeDistanceClient;
    }

    /** Hasil jarak + asalnya: RUTE_ASLI (OSRM) atau PERKIRAAN (Haversine x faktor). */
    public record JarakHasil(BigDecimal jarakKm, String sumber) {
    }

    public double getMaxRadiusKm() {
        return maxRadiusKm;
    }

    /** Hybrid: coba rute asli dulu, gagal -> perkiraan. */
    public JarakHasil hitungJarak(double destLatitude, double destLongitude) {
        return routeDistanceClient
                .jarakKm(warehouseLatitude, warehouseLongitude, destLatitude, destLongitude)
                .map(km -> new JarakHasil(km, "RUTE_ASLI"))
                .orElseGet(() -> new JarakHasil(hitungJarakKm(destLatitude, destLongitude), "PERKIRAAN"));
    }

    // Koordinat toko/gudang Lezzat Frozen Food.
    @Value("${lezzat.warehouse.latitude:-7.756318}")
    private double warehouseLatitude;

    @Value("${lezzat.warehouse.longitude:110.371033}")
    private double warehouseLongitude;

    // Disepakati: 15 km.
    @Value("${lezzat.delivery.max-radius-km:15.0}")
    private double maxRadiusKm;

    // Faktor koreksi supaya jarak garis lurus (Haversine) lebih mendekati
    // jarak jalan sebenarnya. Angka 1.3 adalah perkiraan umum (jalan kota
    // biasanya 1.2x-1.4x lebih panjang dari garis lurus) — sesuaikan kalau
    // ada data pembanding yang lebih akurat.
    @Value("${lezzat.delivery.road-distance-factor:1.3}")
    private double roadDistanceFactor;

    /**
     * Menghitung perkiraan jarak jalan (km) dari toko ke koordinat tujuan.
     * Rumus: Haversine (garis lurus) x faktor koreksi jalan.
     */
    public BigDecimal hitungJarakKm(double destLatitude, double destLongitude) {
        double jarakLurus = haversine(warehouseLatitude, warehouseLongitude, destLatitude, destLongitude);
        double perkiraanJarakJalan = jarakLurus * roadDistanceFactor;
        return BigDecimal.valueOf(perkiraanJarakJalan).setScale(2, RoundingMode.HALF_UP);
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

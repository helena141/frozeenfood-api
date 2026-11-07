package com.lezzat.b2b2c.frozenfood.service.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Semua angka bisnis Smart Delivery ada di sini dan bisa diubah lewat
 * application.yml (prefix: lezzat.delivery) TANPA mengubah kode.
 *
 * Nilai default di bawah = KEPUTUSAN toko (ongkir, jam, radius, jumlah motor)
 * atau ASUMSI sementara (kapasitas motor, kecepatan, waktu persiapan, batas
 * waktu aman barang beku). Yang ASUMSI ditandai jelas di komentar.
 *
 * Catatan: kunci lama lezzat.delivery.max-radius-km dan
 * lezzat.delivery.road-distance-factor tetap dibaca SmartDeliveryService lewat
 * @Value, tidak dipindah ke sini.
 */
@Component
@ConfigurationProperties(prefix = "lezzat.delivery")
@Getter
@Setter
public class DeliveryProperties {

    private Fee fee = new Fee();
    private Motor motor = new Motor();
    private Schedule schedule = new Schedule();
    private Routing routing = new Routing();
    private Unit unit = new Unit();

    /** KEPUTUSAN toko: <=5 km gratis, mulai 6 km Rp10.000, naik Rp1.000/km, maks Rp35.000. */
    @Getter
    @Setter
    public static class Fee {
        private int freeKm = 5;
        private BigDecimal baseFee = new BigDecimal("10000");
        private BigDecimal stepFee = new BigDecimal("1000");
        private BigDecimal maxFee = new BigDecimal("35000");
    }

    @Getter
    @Setter
    public static class Motor {
        /** KEPUTUSAN: 2 motor, 2 pengantar. */
        private int drivers = 2;
        /** ASUMSI (belum ada data owner): kapasitas satu motor. */
        private double maxWeightKg = 15;
        private double maxVolumeLiter = 40;
        /** ASUMSI: kecepatan rata-rata motor dalam kota. */
        private double avgSpeedKmh = 25;
        /** ASUMSI: waktu persiapan/packing sebelum berangkat. */
        private int prepMinutes = 20;
        /** ASUMSI: batas waktu aman barang beku di perjalanan (cooler box). */
        private int maxColdChainMinutes = 60;
        /** ASUMSI: tambahan tunggu bila semua pengantar sedang bertugas. */
        private int queueDelayMinutes = 30;
    }

    /** KEPUTUSAN: buka 09.00-21.00, batas pesan hari yang sama 15.00 (WIB). */
    @Getter
    @Setter
    public static class Schedule {
        private LocalTime open = LocalTime.of(9, 0);
        private LocalTime close = LocalTime.of(21, 0);
        private LocalTime sameDayCutoff = LocalTime.of(15, 0);
        private String zone = "Asia/Jakarta";
    }

    /**
     * Jarak jalan asli lewat OSRM. Default NONAKTIF: aktifkan setelah syarat
     * pemakaian layanan rute publik dicek. Bila aktif tapi gagal/lambat,
     * sistem otomatis memakai Haversine x faktor jalan.
     */
    @Getter
    @Setter
    public static class Routing {
        private boolean enabled = false;
        private String baseUrl = "https://router.project-osrm.org";
        private int timeoutMs = 2000;
    }

    /**
     * ASUMSI: satuan data produk. Kalau products.berat dalam gram, isi
     * weightToKg = 0.001. Kalau products.volume dalam cm3, isi
     * volumeToLiter = 0.001 (1 liter = 1000 cm3); kalau dalam m3, isi 1000.
     */
    @Getter
    @Setter
    public static class Unit {
        private double weightToKg = 1.0;
        private double volumeToLiter = 1.0;
    }
}

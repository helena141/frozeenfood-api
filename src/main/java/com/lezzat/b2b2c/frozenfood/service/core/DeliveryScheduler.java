package com.lezzat.b2b2c.frozenfood.service.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Jadwal dan estimasi tiba (Ide 3).
 *
 * Aturan:
 *  - Pesanan dibuat SEBELUM jam batas (15.00 WIB) -> boleh dikirim hari yang sama.
 *    Tepat pukul 15.00 atau lebih -> besok.
 *  - Berangkat = waktu pesan + waktu persiapan, tapi tidak sebelum jam buka.
 *    Bila semua pengantar sedang bertugas, ditambah waktu tunggu.
 *  - Tiba = berangkat + (jarak / kecepatan). Bila tiba melewati jam tutup,
 *    dijadwalkan besok pagi sejak jam buka.
 *  - Hari libur tidak diperhitungkan (belum ada datanya): "besok" = hari kalender berikutnya.
 *
 * Kelas murni tanpa Spring; waktu dikirim lewat parameter agar mudah diuji.
 */
public class DeliveryScheduler {

    public record Jadwal(boolean hariIni, LocalDateTime estimasiTiba, int menitPerjalanan, String keterangan) {
    }

    private final LocalTime buka;
    private final LocalTime tutup;
    private final LocalTime batasHariIni;
    private final ZoneId zona;
    private final int menitPersiapan;
    private final double kecepatanKmh;
    private final int menitAntrean;

    public DeliveryScheduler(LocalTime buka, LocalTime tutup, LocalTime batasHariIni, ZoneId zona,
                             int menitPersiapan, double kecepatanKmh, int menitAntrean) {
        if (kecepatanKmh <= 0) {
            throw new IllegalArgumentException("Kecepatan rata-rata harus lebih dari 0");
        }
        this.buka = buka;
        this.tutup = tutup;
        this.batasHariIni = batasHariIni;
        this.zona = zona;
        this.menitPersiapan = menitPersiapan;
        this.kecepatanKmh = kecepatanKmh;
        this.menitAntrean = menitAntrean;
    }

    public int menitPerjalanan(BigDecimal jarakKm) {
        return jarakKm.multiply(BigDecimal.valueOf(60))
                .divide(BigDecimal.valueOf(kecepatanKmh), 0, RoundingMode.CEILING)
                .intValueExact();
    }

    public Jadwal jadwalkan(ZonedDateTime waktuPesan, BigDecimal jarakKm, boolean semuaPengantarSibuk) {
        LocalDateTime lokal = waktuPesan.withZoneSameInstant(zona).toLocalDateTime();
        LocalDate hari = lokal.toLocalDate();
        int perjalanan = menitPerjalanan(jarakKm);
        String alasan;

        if (lokal.toLocalTime().isBefore(batasHariIni)) {
            LocalDateTime mulai = lokal.plusMinutes(menitPersiapan);
            LocalDateTime jamBuka = hari.atTime(buka);
            if (mulai.isBefore(jamBuka)) {
                mulai = jamBuka;
            }
            if (semuaPengantarSibuk) {
                mulai = mulai.plusMinutes(menitAntrean);
            }
            LocalDateTime tiba = mulai.plusMinutes(perjalanan);
            if (!tiba.isAfter(hari.atTime(tutup))) {
                String ket = "Dipesan sebelum batas " + batasHariIni + " dan tiba sebelum jam tutup "
                        + tutup + ", dikirim hari ini";
                if (semuaPengantarSibuk) {
                    ket += " (ditambah antrean " + menitAntrean + " menit karena semua pengantar sedang bertugas)";
                }
                return new Jadwal(true, tiba, perjalanan, ket);
            }
            alasan = "Perkiraan tiba melewati jam tutup " + tutup + ", dijadwalkan besok";
        } else {
            alasan = "Dipesan pukul " + lokal.toLocalTime().withNano(0) + " (batas hari yang sama "
                    + batasHariIni + "), dijadwalkan besok";
        }

        LocalDateTime mulaiBesok = hari.plusDays(1).atTime(buka).plusMinutes(menitPersiapan);
        return new Jadwal(false, mulaiBesok.plusMinutes(perjalanan), perjalanan, alasan);
    }
}

package com.lezzat.b2b2c.frozenfood.service.core;

import com.lezzat.b2b2c.frozenfood.exception.InvalidDeliveryStatusException;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Aturan perpindahan status pengiriman (Ide 1: status berurutan).
 *
 * MENUNGGU_KONFIRMASI_ADMIN -> MENUNGGU_PICKUP | DIBATALKAN   (khusus pihak ketiga)
 * MENUNGGU_PICKUP           -> DISIAPKAN | DIBATALKAN
 * DISIAPKAN                 -> DALAM_PERJALANAN | DIBATALKAN
 * DALAM_PERJALANAN          -> TERKIRIM | GAGAL_KIRIM
 * GAGAL_KIRIM               -> DALAM_PERJALANAN | DIBATALKAN
 * TERKIRIM, DIBATALKAN      -> (status akhir, tidak boleh berubah lagi)
 *
 * Kelas ini murni (tanpa Spring) supaya mudah diuji unit.
 */
public final class DeliveryStatusMachine {

    public static final String MENUNGGU_KONFIRMASI_ADMIN = "MENUNGGU_KONFIRMASI_ADMIN";
    public static final String MENUNGGU_PICKUP = "MENUNGGU_PICKUP";
    public static final String DISIAPKAN = "DISIAPKAN";
    public static final String DALAM_PERJALANAN = "DALAM_PERJALANAN";
    public static final String TERKIRIM = "TERKIRIM";
    public static final String GAGAL_KIRIM = "GAGAL_KIRIM";
    public static final String DIBATALKAN = "DIBATALKAN";

    private static final Map<String, Set<String>> TRANSISI = Map.of(
            MENUNGGU_KONFIRMASI_ADMIN, Set.of(MENUNGGU_PICKUP, DIBATALKAN),
            MENUNGGU_PICKUP, Set.of(DISIAPKAN, DIBATALKAN),
            DISIAPKAN, Set.of(DALAM_PERJALANAN, DIBATALKAN),
            DALAM_PERJALANAN, Set.of(TERKIRIM, GAGAL_KIRIM),
            GAGAL_KIRIM, Set.of(DALAM_PERJALANAN, DIBATALKAN),
            TERKIRIM, Set.of(),
            DIBATALKAN, Set.of()
    );

    private DeliveryStatusMachine() {
    }

    /** Merapikan input: trim + huruf besar. Null/kosong ditolak. */
    public static String normalisasi(String status) {
        if (status == null || status.isBlank()) {
            throw new InvalidDeliveryStatusException("Status pengiriman wajib diisi");
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    public static Set<String> semuaStatus() {
        return new TreeSet<>(TRANSISI.keySet());
    }

    public static boolean statusAkhir(String status) {
        Set<String> berikut = TRANSISI.get(status);
        return berikut != null && berikut.isEmpty();
    }

    /** Melempar InvalidDeliveryStatusException kalau perpindahan tidak diizinkan. */
    public static void validasiTransisi(String dari, String ke) {
        String target = normalisasi(ke);
        if (!TRANSISI.containsKey(target)) {
            throw new InvalidDeliveryStatusException(
                    "Status '" + ke + "' tidak dikenal. Status yang valid: " + semuaStatus());
        }
        if (dari == null || !TRANSISI.containsKey(dari)) {
            throw new InvalidDeliveryStatusException(
                    "Status saat ini '" + dari + "' tidak dikenal sehingga tidak bisa diubah ke " + target);
        }
        if (dari.equals(target)) {
            throw new InvalidDeliveryStatusException("Pengiriman sudah berstatus " + dari);
        }
        Set<String> diizinkan = TRANSISI.get(dari);
        if (diizinkan.isEmpty()) {
            throw new InvalidDeliveryStatusException(
                    "Pengiriman berstatus " + dari + " (status akhir) dan tidak dapat diubah lagi");
        }
        if (!diizinkan.contains(target)) {
            throw new InvalidDeliveryStatusException(
                    "Perubahan status " + dari + " -> " + target + " tidak diizinkan. Dari " + dari
                            + " hanya boleh ke: " + new TreeSet<>(diizinkan));
        }
    }
}

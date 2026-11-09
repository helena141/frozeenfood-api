package com.lezzat.b2b2c.frozenfood.service.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Menjumlahkan berat dan volume muatan satu pesanan (Ide 4: muatan & cold chain)
 * lalu mengecek apakah muat di satu motor.
 *
 * Satuan data produk belum dikonfirmasi, jadi dikonversi lewat faktor
 * (lezzat.delivery.unit.*). Data produk yang kosong dihitung 0 dan diberi
 * peringatan, supaya hasil tidak tampak lebih akurat dari kenyataan.
 */
public class MuatanCalculator {

    public record ItemMuatan(String namaProduk, BigDecimal berat, BigDecimal volume, int quantity) {
    }

    public record Muatan(BigDecimal beratKg, BigDecimal volumeLiter, List<String> peringatan) {
    }

    private final double weightToKg;
    private final double volumeToLiter;

    public MuatanCalculator(double weightToKg, double volumeToLiter) {
        this.weightToKg = weightToKg;
        this.volumeToLiter = volumeToLiter;
    }

    public Muatan hitung(List<ItemMuatan> items) {
        BigDecimal totalKg = BigDecimal.ZERO;
        BigDecimal totalLiter = BigDecimal.ZERO;
        Set<String> produkTakLengkap = new LinkedHashSet<>();

        if (items != null) {
            for (ItemMuatan item : items) {
                BigDecimal qty = BigDecimal.valueOf(item.quantity());
                if (item.berat() == null) {
                    produkTakLengkap.add(item.namaProduk());
                } else {
                    totalKg = totalKg.add(item.berat().multiply(qty).multiply(BigDecimal.valueOf(weightToKg)));
                }
                if (item.volume() == null) {
                    produkTakLengkap.add(item.namaProduk());
                } else {
                    totalLiter = totalLiter.add(item.volume().multiply(qty).multiply(BigDecimal.valueOf(volumeToLiter)));
                }
            }
        }

        List<String> peringatan = new ArrayList<>();
        if (!produkTakLengkap.isEmpty()) {
            peringatan.add("Data berat/volume belum lengkap untuk: " + String.join(", ", produkTakLengkap)
                    + " (dihitung 0, muatan bisa lebih besar dari perkiraan)");
        }
        return new Muatan(totalKg.setScale(2, RoundingMode.HALF_UP),
                totalLiter.setScale(2, RoundingMode.HALF_UP), peringatan);
    }

    /** True bila muatan masih muat di SATU motor. */
    public static boolean muatDiSatuMotor(Muatan muatan, double maxKg, double maxLiter) {
        return muatan.beratKg().compareTo(BigDecimal.valueOf(maxKg)) <= 0
                && muatan.volumeLiter().compareTo(BigDecimal.valueOf(maxLiter)) <= 0;
    }
}

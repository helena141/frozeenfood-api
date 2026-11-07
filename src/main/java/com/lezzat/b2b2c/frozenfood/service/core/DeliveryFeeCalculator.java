package com.lezzat.b2b2c.frozenfood.service.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ongkir jalur motor toko (Ide 2).
 *
 * Aturan (KEPUTUSAN toko):
 *  1. Jarak dibulatkan ke km terdekat; pecahan >= 0,5 naik, < 0,5 turun (HALF_UP).
 *  2. km <= freeKm (5)  -> gratis.
 *  3. km >= freeKm + 1  -> baseFee (Rp10.000) pada km pertama di atas batas gratis,
 *     lalu + stepFee (Rp1.000) untuk tiap km berikutnya.
 *  4. Maksimum maxFee (Rp35.000).
 *
 * Contoh: 5 km = 0; 6 km = 10.000; 7 km = 11.000; 15 km = 19.000; >= 31 km = 35.000.
 * Kelas murni tanpa Spring.
 */
public class DeliveryFeeCalculator {

    private final int freeKm;
    private final BigDecimal baseFee;
    private final BigDecimal stepFee;
    private final BigDecimal maxFee;

    public DeliveryFeeCalculator(int freeKm, BigDecimal baseFee, BigDecimal stepFee, BigDecimal maxFee) {
        this.freeKm = freeKm;
        this.baseFee = baseFee;
        this.stepFee = stepFee;
        this.maxFee = maxFee;
    }

    public int bulatkanKm(BigDecimal jarakKm) {
        if (jarakKm == null || jarakKm.signum() < 0) {
            throw new IllegalArgumentException("Jarak tidak boleh kosong atau negatif");
        }
        return jarakKm.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    public BigDecimal hitung(BigDecimal jarakKm) {
        int km = bulatkanKm(jarakKm);
        if (km <= freeKm) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal ongkir = baseFee.add(stepFee.multiply(BigDecimal.valueOf(km - (freeKm + 1L))));
        return ongkir.min(maxFee).setScale(2, RoundingMode.HALF_UP);
    }
}

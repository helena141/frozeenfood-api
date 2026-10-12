package com.lezzat.b2b2c.frozenfood.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

@Service
public class TieringPriceService {

    // Doktrin Diskon Grosir B2B
    private static final int TIER_1_MIN_QTY = 50;
    private static final BigDecimal TIER_1_DISCOUNT = new BigDecimal("0.05"); // Diskon 5% untuk 50+ item

    private static final int TIER_2_MIN_QTY = 100;
    private static final BigDecimal TIER_2_DISCOUNT = new BigDecimal("0.10"); // Diskon 10% untuk 100+ item

    /**
     * Mesin Kalkulator Harga Dinamis
     */
    public BigDecimal calculateFinalPrice(String tipeAkun, int quantity, BigDecimal hargaNormal) {

        // 1. Tembok B2C: Harga mati, tidak ada negosiasi kuantitas
        if ("B2C".equalsIgnoreCase(tipeAkun)) {
            return hargaNormal;
        }

        // 2. Eksekusi B2B: Banting harga berdasarkan volume
        if ("B2B".equalsIgnoreCase(tipeAkun)) {
            if (quantity >= TIER_2_MIN_QTY) {
                return applyDiscount(hargaNormal, TIER_2_DISCOUNT);
            } else if (quantity >= TIER_1_MIN_QTY) {
                return applyDiscount(hargaNormal, TIER_1_DISCOUNT);
            }
        }

        // Default: Kalau B2B beli eceran (di bawah 50 item), kena harga normal
        return hargaNormal;
    }

    /**
     * Presisi Finansial: Potong diskon dan bulatkan ke 2 desimal
     */
    private BigDecimal applyDiscount(BigDecimal basePrice, BigDecimal discountRate) {
        BigDecimal discountAmount = basePrice.multiply(discountRate);
        return basePrice.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
    }
}

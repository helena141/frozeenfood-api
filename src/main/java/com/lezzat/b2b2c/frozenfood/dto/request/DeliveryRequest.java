package com.lezzat.b2b2c.frozenfood.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * metodeDeliv sekarang OPSIONAL: sistem yang memutuskan
 * (SAMEDAY / TERJADWAL untuk motor toko, PIHAK_KETIGA untuk di luar jangkauan).
 * Nilai yang dikirim klien diabaikan.
 */
public record DeliveryRequest(
        @NotNull UUID idOrder,
        @NotBlank String alamatTujuan,
        @NotNull Double latitudeTujuan,
        @NotNull Double longitudeTujuan,
        String metodeDeliv
) {
}

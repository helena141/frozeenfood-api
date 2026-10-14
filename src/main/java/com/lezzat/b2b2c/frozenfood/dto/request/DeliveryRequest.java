package com.lezzat.b2b2c.frozenfood.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeliveryRequest(

        @NotNull(message = "idOrder wajib diisi")
        UUID idOrder,

        @NotBlank(message = "Alamat tujuan wajib diisi")
        String alamatTujuan,

        // Latitude/longitude TIDAK disimpan di entity Delivery — cuma dipakai
        // sementara untuk dihitung SmartDeliveryService, hasil akhirnya
        // (field "jarak") yang disimpan ke database.
        @NotNull(message = "Latitude tujuan wajib diisi")
        Double latitudeTujuan,

        @NotNull(message = "Longitude tujuan wajib diisi")
        Double longitudeTujuan,

        // Contoh nilai: INSTANT, REGULAR, KURIR_TOKO
        @NotBlank(message = "Metode delivery wajib diisi")
        String metodeDeliv
) {
}
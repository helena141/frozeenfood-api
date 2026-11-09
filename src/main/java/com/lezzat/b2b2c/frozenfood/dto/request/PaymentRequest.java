package com.lezzat.b2b2c.frozenfood.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentRequest(

        @NotNull(message = "idOrder wajib diisi")
        UUID idOrder,

        // Contoh nilai: TRANSFER, VA, QRIS, CASH
        @NotBlank(message = "Metode pembayaran wajib diisi")
        String metodePay
) {
}
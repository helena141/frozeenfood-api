package com.lezzat.b2b2c.frozenfood.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

// Nama file ini eksplisit disebut di struktur direktori final DokPI.pdf.
// ASUMSI bentuk payload: sesuaikan field begitu payment gateway asli
// (mis. Midtrans/Xendit) sudah dipilih tim, karena tiap gateway punya
// format callback/webhook yang beda-beda.
public record PaymentCallbackRequest(

        @NotBlank(message = "gatewayRef wajib diisi")
        String gatewayRef,

        // Contoh nilai: LUNAS, GAGAL, EXPIRED
        @NotBlank(message = "Status pembayaran dari gateway wajib diisi")
        String status,

        // Nominal yang dikonfirmasi gateway — dicocokkan ke "jumlah" yang
        // tersimpan sebagai validasi tambahan sebelum status diupdate.
        BigDecimal jumlahDikonfirmasi
) {
}
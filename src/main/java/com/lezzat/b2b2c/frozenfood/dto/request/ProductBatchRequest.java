package com.lezzat.b2b2c.frozenfood.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductBatchRequest(
        @NotNull(message = "ID Produk wajib diisi")
        UUID productId,

        @NotNull(message = "Stok awal wajib diisi")
        @Min(value = 1, message = "Stok awal minimal 1")
        Integer stokAwal,

        @NotNull(message = "Tanggal kedaluwarsa wajib diisi")
        @Future(message = "Tanggal kedaluwarsa harus di masa depan")
        LocalDate tanggalExp,

        BigDecimal hargaOverride,

        BigDecimal diskon
) {
}
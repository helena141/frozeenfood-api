// DRAFT — idealnya di package dto.response (tanggung jawab Bony).
package com.lezzat.b2b2c.frozenfood.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID idProduk,
        String namaProduk,
        String deskripsiProduk,
        String kategori,
        BigDecimal hargaEceran,
        BigDecimal berat,
        BigDecimal volume,
        String gambar,  
        Boolean isActive,
        Integer totalStokOnline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

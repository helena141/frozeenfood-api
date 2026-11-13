// DRAFT — idealnya di package dto.response (tanggung jawab Bony).
package com.lezzat.b2b2c.frozenfood.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        String idProduk,
        String namaProduk,
        String deskripsiProduk,
        String kategori,
        BigDecimal hargaEceran,
        BigDecimal hargaBeli,
        BigDecimal keuntungan,
        BigDecimal berat,
        BigDecimal volume,
        String gambar,  
        Boolean isActive,
        Integer totalStokOnline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public ProductResponse(
            String idProduk,
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
        this(
                idProduk,
                namaProduk,
                deskripsiProduk,
                kategori,
                hargaEceran,
                BigDecimal.ZERO,
                hargaEceran != null ? hargaEceran : BigDecimal.ZERO,
                berat,
                volume,
                gambar,
                isActive,
                totalStokOnline,
                createdAt,
                updatedAt
        );
    }
}

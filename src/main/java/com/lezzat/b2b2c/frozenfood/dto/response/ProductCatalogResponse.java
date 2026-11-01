package com.lezzat.b2b2c.frozenfood.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCatalogResponse {
    private UUID idProduk;
    private String namaProduk;
    private String deskripsiProduk;
    private String kategori;
    private BigDecimal hargaEceran;
    private BigDecimal berat;
    private BigDecimal volume;
    private String gambar;
    private Boolean isActive;
    private Integer totalStock; // Akumulasi stok dari seluruh ProductBatch
}
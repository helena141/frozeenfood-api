package com.lezzat.b2b2c.frozenfood.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    private String namaProduk;
    private String deskripsiProduk;
    private String kategori;
    private BigDecimal hargaEceran;
    private BigDecimal berat;
    private BigDecimal volume;
    private String gambar;
    private Boolean isActive;
}
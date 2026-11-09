package com.lezzat.b2b2c.frozenfood.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
// import java.util.UUID; sudah dihapus karena tidak dipakai lagi

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCatalogResponse {
    private String idProduk; // Diubah dari UUID menjadi String (Contoh: "NGT-001")
    private String namaProduk;
    private String deskripsiProduk;
    private String kategori;
    private BigDecimal hargaEceran;
    private BigDecimal berat;
    private BigDecimal volume;
    private String gambar;
    private Boolean isActive;
    private Integer totalStock; // Akumulasi stok dari seluruh ProductBatch

    // Alias untuk kompatibilitas penuh dengan properti totalStokOnline di Frontend Next.js
    public Integer getTotalStokOnline() {
        return totalStock != null ? totalStock : 0;
    }
}
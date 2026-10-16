package com.lezzat.b2b2c.frozenfood.entity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder


public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_produk", updatable = false, nullable = false)
    private UUID idProduk;

    @Column(name = "nama_produk", nullable = false, length = 200)
    private String namaProduk;

    @Column(name = "deskripsi_produk", columnDefinition = "TEXT")
    private String deskripsiProduk;

    @Column(name = "kategori", nullable = false, length = 100)
    private String kategori;

    // WAJIB BigDecimal untuk finansial, dilarang keras pakai Double/Float
    @Column(name = "harga_eceran", nullable = false, precision = 15, scale = 2)
    private BigDecimal hargaEceran;

    @Column(name = "berat", nullable = false, precision = 10, scale = 2)
    private BigDecimal berat;

    @Column(name = "volume", precision = 10, scale = 2)
    private BigDecimal volume;

    @Column(name = "gambar", columnDefinition = "TEXT")
    private String gambar;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Delegasikan urusan waktu murni ke mesin PostgreSQL (Trigger & Default)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", insertable = false, updatable = false)
    private LocalDateTime deletedAt;
    
    // Relasi ke ProductBatch (Wajib LAZY sesuai Hukum Arsitektur)
    @OneToMany(mappedBy = "product", fetch = jakarta.persistence.FetchType.LAZY)
    private java.util.List<ProductBatch> productBatches;
}

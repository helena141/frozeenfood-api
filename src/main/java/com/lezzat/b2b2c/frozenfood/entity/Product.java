package com.lezzat.b2b2c.frozenfood.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
    @Column(name = "id_produk", updatable = false, nullable = false, length = 15)
    private String idProduk;

    @Column(name = "nama_produk", nullable = false, length = 200)
    private String namaProduk;

    @Column(name = "deskripsi_produk", columnDefinition = "TEXT")
    private String deskripsiProduk;

    @Column(name = "kategori", nullable = false, length = 100)
    private String kategori;

    @Column(name = "harga_eceran", nullable = false, precision = 15, scale = 2)
    private BigDecimal hargaEceran;

    @Builder.Default
    @Column(name = "harga_beli", precision = 15, scale = 2)
    private BigDecimal hargaBeli = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "keuntungan", precision = 15, scale = 2)
    private BigDecimal keuntungan = BigDecimal.ZERO;

    @Column(name = "berat", nullable = false, precision = 10, scale = 2)
    private BigDecimal berat;

    @Column(name = "volume", precision = 10, scale = 2)
    private BigDecimal volume;

    @Column(name = "gambar", columnDefinition = "TEXT")
    private String gambar;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", insertable = false, updatable = false)
    private LocalDateTime deletedAt;
    
    @OneToMany(mappedBy = "product", fetch = jakarta.persistence.FetchType.LAZY)
    private java.util.List<ProductBatch> productBatches;

    @PrePersist
    @PreUpdate
    public void calculateKeuntungan() {
        if (this.hargaBeli == null) {
            this.hargaBeli = BigDecimal.ZERO;
        }
        if (this.hargaEceran != null) {
            this.keuntungan = this.hargaEceran.subtract(this.hargaBeli);
        } else {
            this.keuntungan = BigDecimal.ZERO;
        }
    }
}
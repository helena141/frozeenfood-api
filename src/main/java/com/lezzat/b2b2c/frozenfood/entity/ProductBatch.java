package com.lezzat.b2b2c.frozenfood.entity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "produk_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE produk_batches SET deleted_at = CURRENT_TIMESTAMP WHERE id_prod_batch = ?")
@SQLRestriction("deleted_at IS NULL")
public class ProductBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_prod_batch", updatable = false, nullable = false)
    private UUID idProdBatch;

    // RELASI KRUSIAL: Menunjuk kembali ke entitas induk (Product)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_produk", nullable = false)
    private Product product;

    @Column(name = "stok_awal", nullable = false)
    private Integer stokAwal;

    @Column(name = "stok_online", nullable = false)
    private Integer stokOnline;

    // INDIKATOR FEFO: Wajib LocalDate karena kita hanya peduli tanggal, bukan jam
    @Column(name = "tanggal_exp", nullable = false)
    private LocalDate tanggalExp; 

    // WAJIB BigDecimal untuk perhitungan finansial tingkat enterprise
    @Column(name = "harga_override", precision = 15, scale = 2)
    private BigDecimal hargaOverride;

    @Column(name = "diskon", precision = 5, scale = 2)
    private BigDecimal diskon;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", insertable = false, updatable = false)
    private LocalDateTime deletedAt;
}

    



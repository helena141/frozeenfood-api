package com.lezzat.b2b2c.frozenfood.entity;

import java.math.BigDecimal;
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
@Table(name = "tiering_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE tiering_prices SET deleted_at = CURRENT_TIMESTAMP WHERE id_tiering = ?")
@SQLRestriction("deleted_at IS NULL")
public class TieringPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_tiering", updatable = false, nullable = false)
    private UUID idTiering;

    // RELASI KRUSIAL: Menunjuk ke Produk sebagai pemilik aturan harga
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_produk", nullable = false)
    private Product product;

    @Column(name = "min_qty", nullable = false)
    private Integer minQty;

    @Column(name = "max_qty", nullable = false)
    private Integer maxQty;

    // WAJIB BigDecimal untuk akurasi uang
    @Column(name = "harga_per_unit", nullable = false, precision = 15, scale = 2)
    private BigDecimal hargaPerUnit;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", insertable = false, updatable = false)
    private LocalDateTime deletedAt;
}

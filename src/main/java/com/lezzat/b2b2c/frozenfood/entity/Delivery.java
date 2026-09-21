package com.lezzat.b2b2c.frozenfood.entity;

// Entity ini cocok dengan tabel "deliveries" di V1__init_schema.sql
// + V2__add_deleted_at_deliveries.sql.
// Kolom DB: id_deliv, id_order, metode_deliv, alamat_tujuan, jarak,
//           status_delivery, created_at, updated_at, deleted_at.

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE deliveries SET deleted_at = CURRENT_TIMESTAMP WHERE id_deliv = ?")
@SQLRestriction("deleted_at IS NULL")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_deliv", updatable = false, nullable = false)
    private UUID idDelivery;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_order", nullable = false, unique = true)
    private Order order;

    @Column(name = "metode_deliv", length = 100, nullable = false)
    private String metodeDeliv;

    @Column(name = "alamat_tujuan", columnDefinition = "TEXT", nullable = false)
    private String alamatTujuan;

    // Catatan: precision/scale SENGAJA tidak dipasang di sini karena tipe
    // Java-nya Float (bukan BigDecimal) — precision/scale itu atribut untuk
    // kolom NUMERIC/DECIMAL, bukan FLOAT/REAL. Kalau ddl-auto: validate dan
    // masih error soal tipe kolom, cek tipe kolom "jarak" di V1 (harus
    // FLOAT/REAL/DOUBLE PRECISION, bukan NUMERIC).
    @Column(name = "jarak")
    private Float jarak;

    @Column(name = "status_delivery", length = 50, nullable = false)
    private String statusDelivery;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // Tidak insertable=false/updatable=false — kolom ini diisi manual dari
    // service layer setiap ada perubahan (lihat DeliveryService).
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", insertable = false, updatable = false)
    private LocalDateTime deletedAt;
}
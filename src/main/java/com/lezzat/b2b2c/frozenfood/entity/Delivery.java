package com.lezzat.b2b2c.frozenfood.entity;

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
// KOREKSI ARSITEK: Memperbaiki kesalahan Krisna dari id_deliv menjadi id_delivery agar sinkron dengan database
@SQLDelete(sql = "UPDATE deliveries SET deleted_at = CURRENT_TIMESTAMP WHERE id_delivery = ?")
@SQLRestriction("deleted_at IS NULL")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_delivery", updatable = false, nullable = false)
    private UUID idDelivery;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_order", nullable = false, unique = true)
    private Order order;

    @Column(name = "metode_deliv", length = 100, nullable = false)
    private String metodeDeliv;

    @Column(name = "alamat_tujuan", columnDefinition = "TEXT", nullable = false)
    private String alamatTujuan;

    @Column(name = "jarak")
    private Float jarak;

    @Column(name = "status_delivery", length = 50, nullable = false)
    private String statusDelivery;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", insertable = false, updatable = false)
    private LocalDateTime deletedAt;
}
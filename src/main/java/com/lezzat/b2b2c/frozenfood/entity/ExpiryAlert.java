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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "expiry_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE expiry_alerts SET deleted_at = CURRENT_TIMESTAMP WHERE id_alert = ?")
@SQLRestriction("deleted_at IS NULL")
public class ExpiryAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_alert", updatable = false, nullable = false)
    private UUID idAlert;

    // Relasi Mutlak: Peringatan ini milik batch fisik yang mana?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prod_batch", nullable = false)
    private ProductBatch productBatch;

    @Column(name = "pesan", nullable = false)
    private String pesan;

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isResolved = false; // Default: Peringatan belum ditangani
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
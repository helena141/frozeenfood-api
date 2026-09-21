package com.lezzat.b2b2c.frozenfood.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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
@Table(name = "expiry_alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLDelete(sql = "UPDATE expiry_alerts SET deleted_at = CURRENT_TIMESTAMP WHERE id_alert = ?")
@SQLRestriction("deleted_at IS NULL")
public class ExpiryAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idAlert;

   @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prod_batch")
    private ProductBatch productBatch;

    private Integer hariSebelum;
    private LocalDateTime sendAt;
    private Boolean isSent;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
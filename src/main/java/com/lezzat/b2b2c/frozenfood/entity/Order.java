package com.lezzat.b2b2c.frozenfood.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import java.util.List;
import java.util.ArrayList;
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
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE orders SET deleted_at = CURRENT_TIMESTAMP WHERE id_order = ?")
@SQLRestriction("deleted_at IS NULL")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_order", updatable = false, nullable = false)
    private UUID idOrder;

    // Relasi ke Siapa yang Beli
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false)
    private Customer customer;

    // B2C (Eceran) atau B2B (Grosir)
    @Column(name = "tipe_order", nullable = false, length = 20)
    private String tipeOrder;

    // Status: Menunggu Pembayaran, Dibayar, Diproses, Dikirim, Selesai
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "ongkir", nullable = false, precision = 15, scale = 2)
    private BigDecimal ongkir;

    @Column(name = "catatan", columnDefinition = "TEXT")
    private String catatan;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalTagihan;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", insertable = false, updatable = false)
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}

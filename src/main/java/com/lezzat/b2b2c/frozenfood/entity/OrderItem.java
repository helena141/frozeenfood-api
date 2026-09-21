package com.lezzat.b2b2c.frozenfood.entity;

import java.math.BigDecimal;
import java.util.UUID;

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
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_order_item", updatable = false, nullable = false)
    private UUID idOrderItem;

    // Relasi: Baris ini milik Struk yang mana?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_order", nullable = false)
    private Order order;

    // Relasi FEFO Mutlak: Potong stok dari Batch yang mana?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prod_batch", nullable = false)
    private ProductBatch productBatch;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Harga Momen Transaksi (Mencegah nota lama berubah kalau harga master naik)
    @Column(name = "harga_satuan", nullable = false, precision = 15, scale = 2)
    private BigDecimal hargaSatuanSnapshot;

    @Column(name = "subtotal_item", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotalItem;
}
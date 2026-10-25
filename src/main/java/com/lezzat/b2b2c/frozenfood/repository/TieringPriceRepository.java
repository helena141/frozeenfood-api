package com.lezzat.b2b2c.frozenfood.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lezzat.b2b2c.frozenfood.entity.TieringPrice;

@Repository
public interface TieringPriceRepository extends JpaRepository<TieringPrice, UUID> {

    /**
     * Mesin Pencari Tiering Dinamis B2B.
     * Mencari aturan harga grosir yang secara persis mencakup volume pembelian (Quantity).
     */
    @Query("SELECT t FROM TieringPrice t WHERE t.product.idProduk = :productId AND :quantity >= t.minQty AND :quantity <= t.maxQty")
    Optional<TieringPrice> findDynamicB2BPrice(
        @Param("productId") UUID productId, 
        @Param("quantity") int quantity
    );
}
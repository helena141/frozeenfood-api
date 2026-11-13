package com.lezzat.b2b2c.frozenfood.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lezzat.b2b2c.frozenfood.entity.Customer;
import com.lezzat.b2b2c.frozenfood.entity.Order;
import com.lezzat.b2b2c.frozenfood.entity.OrderItem;
import com.lezzat.b2b2c.frozenfood.entity.Product;
import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;
import com.lezzat.b2b2c.frozenfood.entity.TieringPrice;
import com.lezzat.b2b2c.frozenfood.repository.ProductBatchRepository;
import com.lezzat.b2b2c.frozenfood.repository.TieringPriceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TieringPriceService {

    public static final int CLEARANCE_DAYS_THRESHOLD = 7;
    public static final int NEAR_EXPIRY_DAYS_THRESHOLD = 30;
    public static final BigDecimal CLEARANCE_DISCOUNT_PCT = new BigDecimal("50.00");
    public static final BigDecimal NEAR_EXPIRY_DISCOUNT_PCT = new BigDecimal("20.00");
    private static final BigDecimal B2B_REGULAR_DISCOUNT_MULTIPLIER = new BigDecimal("0.92");

    private final TieringPriceRepository tieringPriceRepository;
    private final ProductBatchRepository productBatchRepository;

    public record BatchPriceEvaluation(
            BigDecimal hargaAsli,
            BigDecimal hargaEfektif,
            BigDecimal diskonPersen,
            long sisaHariExpired,
            boolean isDealBatch,
            String tipeDeal,
            String labelDeal
    ) {}

    public record AppliedPriceResult(
            BigDecimal hargaAsli,
            BigDecimal hargaSatuanFinal,
            BigDecimal diskonPersen,
            String keteranganPromo
    ) {}

    public record BatchDeduction(
            ProductBatch batch,
            int quantityDeducted,
            BigDecimal rawUnitPrice,
            BigDecimal rawSubtotal,
            String keteranganPromo
    ) {}

    public record B2BCheckoutResult(
            String productId,
            int totalQuantity,
            BigDecimal blendedUnitPrice,
            BigDecimal totalCost,
            List<BatchDeduction> deductedBatches
    ) {}

    /**
     * Satu-satunya sumber kebenaran (Single Source of Truth) untuk menghitung
     * harga efektif sebuah ProductBatch (dipakai oleh Katalog, B2C, B2B, dan Nota PDF).
     *
     * Aturan Opsi 1 (Otomatis Berjenjang + Override Admin):
     * - Jika sisa hari <= 7 hari (Clearance): diskon minimal 50% (jika hargaOverride kosong).
     * - Jika sisa hari 8..30 hari (FEFO Deal): diskon otomatis 20% (jika diskon & hargaOverride kosong).
     * - Jika Admin mengisi hargaOverride dan/atau diskon secara eksplisit, nilai tersebut dihitung.
     */
    public BatchPriceEvaluation evaluateBatchPrice(Product product, ProductBatch batch) {
        BigDecimal hargaAsli = (product != null && product.getHargaEceran() != null)
                ? product.getHargaEceran().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long daysLeft = batch.getTanggalExp() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), batch.getTanggalExp())
                : 999L;

        BigDecimal hargaDasar = (batch.getHargaOverride() != null
                && batch.getHargaOverride().compareTo(BigDecimal.ZERO) > 0)
                ? batch.getHargaOverride()
                : hargaAsli;

        BigDecimal configuredDiskon = (batch.getDiskon() != null
                && batch.getDiskon().compareTo(BigDecimal.ZERO) > 0)
                ? batch.getDiskon()
                : BigDecimal.ZERO;

        BigDecimal effectiveDiskon = configuredDiskon;
        if (batch.getHargaOverride() == null) {
            if (daysLeft <= CLEARANCE_DAYS_THRESHOLD
                    && effectiveDiskon.compareTo(CLEARANCE_DISCOUNT_PCT) < 0) {
                effectiveDiskon = CLEARANCE_DISCOUNT_PCT;
            } else if (daysLeft <= NEAR_EXPIRY_DAYS_THRESHOLD
                    && effectiveDiskon.compareTo(BigDecimal.ZERO) <= 0) {
                effectiveDiskon = NEAR_EXPIRY_DISCOUNT_PCT;
            }
        }

        BigDecimal hargaEfektif = hargaDasar;
        if (effectiveDiskon.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal faktor = BigDecimal.ONE.subtract(
                    effectiveDiskon.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            hargaEfektif = hargaDasar.multiply(faktor).setScale(2, RoundingMode.HALF_UP);
        } else {
            hargaEfektif = hargaDasar.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalDiskonPct = BigDecimal.ZERO;
        if (hargaAsli.compareTo(BigDecimal.ZERO) > 0 && hargaEfektif.compareTo(hargaAsli) < 0) {
            totalDiskonPct = hargaAsli.subtract(hargaEfektif)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(hargaAsli, 0, RoundingMode.HALF_UP);
        }

        boolean isDeal = (daysLeft <= NEAR_EXPIRY_DAYS_THRESHOLD)
                || (hargaEfektif.compareTo(hargaAsli) < 0);

        String tipeDeal = null;
        String labelDeal = null;
        if (daysLeft <= CLEARANCE_DAYS_THRESHOLD) {
            tipeDeal = "CLEARANCE";
            labelDeal = "Clearance H-7 (-" + totalDiskonPct.stripTrailingZeros().toPlainString() + "%)";
        } else if (daysLeft <= NEAR_EXPIRY_DAYS_THRESHOLD) {
            tipeDeal = "FEFO_DEAL";
            labelDeal = "FEFO Deal H-30 (-" + totalDiskonPct.stripTrailingZeros().toPlainString() + "%)";
        } else if (hargaEfektif.compareTo(hargaAsli) < 0) {
            tipeDeal = "PROMO_BATCH";
            labelDeal = "Promo Batch (-" + totalDiskonPct.stripTrailingZeros().toPlainString() + "%)";
        }

        return new BatchPriceEvaluation(
                hargaAsli,
                hargaEfektif,
                totalDiskonPct,
                Math.max(0L, daysLeft),
                isDeal,
                tipeDeal,
                labelDeal
        );
    }

    /**
     * Mencari aturan TieringPrice yang cocok dengan kuantitas pembelian.
     * Jika kuantitas melebihi maxQty tertinggi yang terdaftar, otomatis gunakan tier tertinggi.
     */
    public Optional<TieringPrice> findApplicableTierPrice(String productId, int quantity) {
        Optional<TieringPrice> exact = tieringPriceRepository.findDynamicB2BPrice(productId, quantity);
        if (exact.isPresent()) {
            return exact;
        }
        return tieringPriceRepository.findByProduct_IdProdukOrderByMinQtyAsc(productId).stream()
                .filter(t -> t.getMinQty() != null && quantity >= t.getMinQty())
                .max(Comparator.comparingInt(TieringPrice::getMinQty));
    }

    /**
     * Menghitung harga satuan final saat pembuatan pesanan (baik B2C maupun B2B),
     * mengambil harga terbaik (paling hemat bagi pelanggan) antara:
     * 1) Harga Deal Batch FEFO (Clearance H-7 / FEFO Deal H-30 / hargaOverride)
     * 2) Harga Tiering Grosir (jika kuantitas memenuhi minQty tiering, atau diskon reguler B2B)
     */
    public AppliedPriceResult calculateOrderItemUnitPrice(
            Product product,
            ProductBatch batch,
            int totalProductQuantity,
            String tipeAkun) {

        BatchPriceEvaluation batchEval = evaluateBatchPrice(product, batch);
        BigDecimal hargaAsli = batchEval.hargaAsli();
        BigDecimal bestPrice = batchEval.hargaEfektif();
        String keterangan = batchEval.labelDeal() != null ? batchEval.labelDeal() : "Harga Normal";

        Optional<TieringPrice> tierOpt = findApplicableTierPrice(product.getIdProduk(), totalProductQuantity);
        if (tierOpt.isPresent()) {
            TieringPrice tier = tierOpt.get();
            BigDecimal tierUnitPrice = tier.getHargaPerUnit().setScale(2, RoundingMode.HALF_UP);
            if (tierUnitPrice.compareTo(bestPrice) < 0) {
                bestPrice = tierUnitPrice;
                keterangan = "Harga Grosir Tier " + tier.getMinQty()
                        + (tier.getMaxQty() != null ? "-" + tier.getMaxQty() : "+") + " pcs";
            }
        } else if ("B2B".equalsIgnoreCase(tipeAkun)) {
            BigDecimal b2bRegular = hargaAsli.multiply(B2B_REGULAR_DISCOUNT_MULTIPLIER)
                    .setScale(2, RoundingMode.HALF_UP);
            if (b2bRegular.compareTo(bestPrice) < 0) {
                bestPrice = b2bRegular;
                keterangan = "Diskon Mitra B2B (-8%)";
            }
        }

        BigDecimal diskonPersen = BigDecimal.ZERO;
        if (hargaAsli.compareTo(BigDecimal.ZERO) > 0 && bestPrice.compareTo(hargaAsli) < 0) {
            diskonPersen = hargaAsli.subtract(bestPrice)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(hargaAsli, 0, RoundingMode.HALF_UP);
        }

        return new AppliedPriceResult(hargaAsli, bestPrice, diskonPersen, keterangan);
    }

    public BigDecimal calculateFinalPrice(String tipeAkun, int quantity, ProductBatch batch) {
        return calculateOrderItemUnitPrice(batch.getProduct(), batch, quantity, tipeAkun)
                .hargaSatuanFinal();
    }

    @Transactional
    public B2BCheckoutResult executeFefoBlendedPricing(Customer customer, String productId, int requestedQty) {
        if (requestedQty <= 0) {
            throw new IllegalArgumentException("Kuantitas order harus lebih dari 0.");
        }

        List<ProductBatch> batches = productBatchRepository
                .findByProduct_IdProdukAndStokOnlineGreaterThanOrderByTanggalExpAsc(productId, 0);

        if (batches.isEmpty()) {
            throw new IllegalStateException("Stok tidak tersedia untuk produk ID: " + productId);
        }

        int totalAvailableStock = batches.stream()
                .mapToInt(batch -> batch.getStokOnline() != null ? batch.getStokOnline() : 0)
                .sum();

        if (totalAvailableStock < requestedQty) {
            throw new IllegalStateException(
                    "Stok tidak mencukupi. Diminta: " + requestedQty + ", Tersedia: " + totalAvailableStock);
        }

        Product product = batches.get(0).getProduct();
        String tipeAkun = customer != null && customer.getTipeAkun() != null ? customer.getTipeAkun() : "B2B";

        int remainingQtyToFulfill = requestedQty;
        BigDecimal totalHarga = BigDecimal.ZERO;
        List<BatchDeduction> deductedBatches = new ArrayList<>();

        for (ProductBatch batch : batches) {
            if (remainingQtyToFulfill <= 0) {
                break;
            }

            int availableInBatch = batch.getStokOnline();
            int qtyTaken = Math.min(availableInBatch, remainingQtyToFulfill);

            batch.setStokOnline(availableInBatch - qtyTaken);
            productBatchRepository.save(batch);

            remainingQtyToFulfill -= qtyTaken;
            BigDecimal qtyTakenDec = BigDecimal.valueOf(qtyTaken);

            AppliedPriceResult applied = calculateOrderItemUnitPrice(product, batch, requestedQty, tipeAkun);
            BigDecimal appliedBatchUnitPrice = applied.hargaSatuanFinal();
            BigDecimal rawSubtotal = appliedBatchUnitPrice.multiply(qtyTakenDec).setScale(2, RoundingMode.HALF_UP);

            totalHarga = totalHarga.add(rawSubtotal);
            deductedBatches.add(new BatchDeduction(
                    batch,
                    qtyTaken,
                    appliedBatchUnitPrice,
                    rawSubtotal,
                    applied.keteranganPromo()
            ));
        }

        BigDecimal hargaRataRataPerPcs = totalHarga.divide(
                BigDecimal.valueOf(requestedQty), 2, RoundingMode.HALF_UP);

        return new B2BCheckoutResult(
                productId,
                requestedQty,
                hargaRataRataPerPcs,
                totalHarga.setScale(2, RoundingMode.HALF_UP),
                deductedBatches
        );
    }

    @Transactional
    public List<OrderItem> processB2BCheckoutItems(Customer customer, Order order, String productId, int requestedQty) {
        B2BCheckoutResult result = executeFefoBlendedPricing(customer, order != null ? productId : productId, requestedQty);
        List<OrderItem> orderItems = new ArrayList<>();

        for (BatchDeduction deduction : result.deductedBatches()) {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productBatch(deduction.batch())
                    .quantity(deduction.quantityDeducted())
                    .hargaSatuanSnapshot(deduction.rawUnitPrice())
                    .subtotalItem(deduction.rawSubtotal())
                    .build();

            orderItems.add(item);
        }

        return orderItems;
    }
}
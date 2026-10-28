package com.lezzat.b2b2c.frozenfood.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    private static final int CLEARANCE_DAYS_THRESHOLD = 7;
    private static final BigDecimal CLEARANCE_DISCOUNT_MULTIPLIER = new BigDecimal("0.50");
    private static final BigDecimal B2B_REGULAR_DISCOUNT_MULTIPLIER = new BigDecimal("0.92");

    private final TieringPriceRepository tieringPriceRepository;
    private final ProductBatchRepository productBatchRepository;

    public record BatchDeduction(
            ProductBatch batch,
            int quantityDeducted,
            BigDecimal rawUnitPrice,
            BigDecimal rawSubtotal
    ) {}

    public record B2BCheckoutResult(
            UUID productId,
            int totalQuantity,
            BigDecimal blendedUnitPrice,
            BigDecimal totalCost,
            List<BatchDeduction> deductedBatches
    ) {}

    public BigDecimal calculateFinalPrice(String tipeAkun, int quantity, ProductBatch batch) {
        BigDecimal hargaDasar = batch.getHargaOverride() != null
                ? batch.getHargaOverride()
                : batch.getProduct().getHargaEceran();

        LocalDate batasCuciGudang = LocalDate.now().plusDays(CLEARANCE_DAYS_THRESHOLD);
        if (!batch.getTanggalExp().isAfter(batasCuciGudang)) {
            log.warn("🚨 [CUCI GUDANG] Batch {} mendekati expired! Harga dipotong 50%.", batch.getIdProdBatch());
            return hargaDasar.multiply(CLEARANCE_DISCOUNT_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
        }

        if ("B2C".equalsIgnoreCase(tipeAkun)) {
            return hargaDasar.setScale(2, RoundingMode.HALF_UP);
        }

        if ("B2B".equalsIgnoreCase(tipeAkun)) {
            Optional<TieringPrice> tiering = tieringPriceRepository.findDynamicB2BPrice(
                    batch.getProduct().getIdProduk(),
                    quantity
            );

            if (tiering.isPresent()) {
                return tiering.get().getHargaPerUnit().setScale(2, RoundingMode.HALF_UP);
            }

            return hargaDasar.multiply(B2B_REGULAR_DISCOUNT_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
        }

        return hargaDasar.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public B2BCheckoutResult executeFefoBlendedPricing(Customer customer, UUID productId, int requestedQty) {
        if (customer == null || !"B2B".equalsIgnoreCase(customer.getTipeAkun())) {
            throw new IllegalArgumentException("Akses ditolak: Kalkulasi ini khusus untuk akun tipe B2B.");
        }

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
        BigDecimal hargaDasarB2C = product.getHargaEceran();
        LocalDate batasCuciGudang = LocalDate.now().plusDays(CLEARANCE_DAYS_THRESHOLD);

        BigDecimal hargaRegulerB2B = tieringPriceRepository
                .findDynamicB2BPrice(productId, requestedQty)
                .map(TieringPrice::getHargaPerUnit)
                .orElseGet(() -> hargaDasarB2C.multiply(B2B_REGULAR_DISCOUNT_MULTIPLIER));

        int remainingQtyToFulfill = requestedQty;
        BigDecimal totalCostClearance = BigDecimal.ZERO;
        BigDecimal totalCostRegular = BigDecimal.ZERO;
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

            boolean isClearanceBatch = !batch.getTanggalExp().isAfter(batasCuciGudang);
            BigDecimal appliedBatchUnitPrice;

            if (isClearanceBatch) {
                appliedBatchUnitPrice = (batch.getHargaOverride() != null)
                        ? batch.getHargaOverride()
                        : hargaDasarB2C.multiply(CLEARANCE_DISCOUNT_MULTIPLIER);

                log.warn("🚨 [B2B CUCI GUDANG] Mengambil {} pcs dari Batch {} (Exp: {}) dengan harga clearance {}",
                        qtyTaken, batch.getIdProdBatch(), batch.getTanggalExp(), appliedBatchUnitPrice);

                totalCostClearance = totalCostClearance.add(appliedBatchUnitPrice.multiply(qtyTakenDec));
            } else {
                appliedBatchUnitPrice = (batch.getHargaOverride() != null)
                        ? batch.getHargaOverride()
                        : hargaRegulerB2B;

                totalCostRegular = totalCostRegular.add(appliedBatchUnitPrice.multiply(qtyTakenDec));
            }

            BigDecimal rawSubtotal = appliedBatchUnitPrice.multiply(qtyTakenDec).setScale(2, RoundingMode.HALF_UP);
            deductedBatches.add(new BatchDeduction(batch, qtyTaken, appliedBatchUnitPrice, rawSubtotal));
        }

        BigDecimal totalHarga = totalCostClearance.add(totalCostRegular)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal hargaRataRataPerPcs = totalHarga.divide(
                BigDecimal.valueOf(requestedQty), 2, RoundingMode.HALF_UP);

        return new B2BCheckoutResult(
                productId,
                requestedQty,
                hargaRataRataPerPcs,
                totalHarga,
                deductedBatches
        );
    }

    @Transactional
    public List<OrderItem> processB2BCheckoutItems(Customer customer, Order order, UUID productId, int requestedQty) {
        B2BCheckoutResult result = executeFefoBlendedPricing(customer, productId, requestedQty);
        List<OrderItem> orderItems = new ArrayList<>();

        for (BatchDeduction deduction : result.deductedBatches()) {
            BigDecimal subtotalItem = result.blendedUnitPrice()
                    .multiply(BigDecimal.valueOf(deduction.quantityDeducted()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productBatch(deduction.batch())
                    .quantity(deduction.quantityDeducted())
                    .hargaSatuanSnapshot(result.blendedUnitPrice())
                    .subtotalItem(subtotalItem)
                    .build();

            orderItems.add(item);
        }

        return orderItems;
    }
}
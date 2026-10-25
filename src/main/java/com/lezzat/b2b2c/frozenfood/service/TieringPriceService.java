package com.lezzat.b2b2c.frozenfood.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
@RequiredArgsConstructor // Standar Enterprise: Inject Repository tanpa @Autowired manual
public class TieringPriceService {

    private static final int CLEARANCE_DAYS_THRESHOLD = 7;
    private static final BigDecimal CLEARANCE_DISCOUNT_MULTIPLIER = new BigDecimal("0.50"); // Harga banting 50%
    private static final BigDecimal B2B_REGULAR_DISCOUNT_MULTIPLIER = new BigDecimal("0.92"); // Diskon reguler B2B 8% (antara 5-10%)

    private final TieringPriceRepository tieringPriceRepository;
    private final ProductBatchRepository productBatchRepository;

    /**
     * Mesin Kalkulator Smart System (FEFO Clearance + Dynamic Tiering)
     */
    public BigDecimal calculateFinalPrice(String tipeAkun, int quantity, ProductBatch batch) {
        
        // Tarik harga normal sebagai fondasi
        BigDecimal hargaDasar = batch.getHargaOverride() != null ? 
                                batch.getHargaOverride() : 
                                batch.getProduct().getHargaEceran();

        // 1. SENSOR DARURAT (CUCI GUDANG FEFO)
        // Jika umur barang tinggal 7 hari atau kurang, banting harga 50% untuk selamatkan aset
        LocalDate batasCuciGudang = LocalDate.now().plusDays(7);
        if (!batch.getTanggalExp().isAfter(batasCuciGudang)) {
            log.warn("🚨 [CUCI GUDANG] Batch {} mendekati expired! Harga dipotong 50%.", batch.getIdProdBatch());
            return hargaDasar.multiply(new BigDecimal("0.50")); 
        }

        // 2. TEMBOK B2C (Aturan Mutlak: Haram dapat diskon kuantitas)
        if ("B2C".equalsIgnoreCase(tipeAkun)) {
            return hargaDasar; 
        }

        // 3. MESIN TIERING B2B (Database-Driven)
        if ("B2B".equalsIgnoreCase(tipeAkun)) {
            Optional<TieringPrice> tiering = tieringPriceRepository.findDynamicB2BPrice(
                batch.getProduct().getIdProduk(), 
                quantity
            );

            // Jika kuantitas pembelian masuk ke dalam rentang min_qty & max_qty di tabel
            if (tiering.isPresent()) {
                return tiering.get().getHargaPerUnit();
            }
        }

        // Fallback: Kalau B2B beli di bawah batas minimum (misal batas min 10, dia beli 2), kena harga dasar
        return hargaDasar;
    }

    /**
     * Algoritma Kalkulasi Harga & Pemotongan Stok Lintas Batch (FEFO + Average Cost) khusus B2B
     */
    @Transactional
    public OrderItem processB2BCheckoutItem(Customer customer, Order order, UUID productId, int requestedQty) {

        // 1. Gatekeeper: Validasi mutlak akun B2B
        if (customer == null || !"B2B".equalsIgnoreCase(customer.getTipeAkun())) {
            throw new IllegalArgumentException("Akses ditolak: Kalkulasi ini khusus untuk akun tipe B2B.");
        }

        if (requestedQty <= 0) {
            throw new IllegalArgumentException("Kuantitas order harus lebih dari 0.");
        }

        // Tarik semua batch aktif untuk produk terkait dengan urutan FEFO (Expired terdekat lebih dulu)
        List<ProductBatch> batches = productBatchRepository
                .findByProduct_IdProdukAndStokOnlineGreaterThanOrderByTanggalExpAsc(productId, 0);

        int totalAvailableStock = batches.stream()
                .mapToInt(ProductBatch::getStokOnline)
                .sum();

        if (totalAvailableStock < requestedQty) {
            throw new IllegalStateException(
                    "Stok tidak mencukupi. Diminta: " + requestedQty + ", Tersedia: " + totalAvailableStock);
        }

        Product product = batches.get(0).getProduct();
        BigDecimal hargaDasarB2C = product.getHargaEceran();
        LocalDate batasCuciGudang = LocalDate.now().plusDays(CLEARANCE_DAYS_THRESHOLD);

        int remainingQtyToFulfill = requestedQty;
        ProductBatch primaryBatchReference = batches.get(0);

        BigDecimal totalCostClearance = BigDecimal.ZERO;
        BigDecimal totalCostRegular = BigDecimal.ZERO;

        // 2 & 3. Loop Pemotongan Stok FEFO (Fase 1: Cuci Gudang -> Fase 2: Reguler)
        for (ProductBatch batch : batches) {
            if (remainingQtyToFulfill <= 0) {
                break;
            }

            int availableInBatch = batch.getStokOnline();
            int qtyTaken = Math.min(availableInBatch, remainingQtyToFulfill);

            // Potong stok batch secara langsung
            batch.setStokOnline(availableInBatch - qtyTaken);
            productBatchRepository.save(batch);

            remainingQtyToFulfill -= qtyTaken;
            BigDecimal qtyTakenDec = BigDecimal.valueOf(qtyTaken);

            boolean isClearanceBatch = !batch.getTanggalExp().isAfter(batasCuciGudang);

            if (isClearanceBatch) {
                // Fase 1: Prioritas Cuci Gudang (Gunakan hargaOverride jika ada, atau potong 50% dari harga dasar)
                BigDecimal hargaGudang = (batch.getHargaOverride() != null)
                        ? batch.getHargaOverride()
                        : hargaDasarB2C.multiply(CLEARANCE_DISCOUNT_MULTIPLIER);

                log.warn("🚨 [B2B CUCI GUDANG] Mengambil {} pcs dari Batch {} dengan harga clearance {}",
                        qtyTaken, batch.getIdProdBatch(), hargaGudang);

                totalCostClearance = totalCostClearance.add(hargaGudang.multiply(qtyTakenDec));
            } else {
                // Fase 2: Stok Batch Reguler (Diskon B2B Bulk pada harga dasar B2C)
                BigDecimal hargaDiskonReguler = hargaDasarB2C.multiply(B2B_REGULAR_DISCOUNT_MULTIPLIER);

                totalCostRegular = totalCostRegular.add(hargaDiskonReguler.multiply(qtyTakenDec));
            }
        }

        // 4. Agregasi Average Cost
        // Total Harga = (Qty Gudang * Harga Gudang) + (Qty Reguler * Harga Diskon Reguler)
        BigDecimal totalHarga = totalCostClearance.add(totalCostRegular)
                .setScale(2, RoundingMode.HALF_UP);

        // Harga Rata-Rata Per Pcs = Total Harga / Total Qty Order
        BigDecimal hargaRataRataPerPcs = totalHarga.divide(
                BigDecimal.valueOf(requestedQty), 2, RoundingMode.HALF_UP);

        // 5. Finalisasi Struk: Kunci harga rata-rata ke hargaSatuanSnapshot pada OrderItem
        return OrderItem.builder()
                .order(order)
                .productBatch(primaryBatchReference)
                .quantity(requestedQty)
                .hargaSatuanSnapshot(hargaRataRataPerPcs)
                .subtotalItem(totalHarga)
                .build();
    }
}
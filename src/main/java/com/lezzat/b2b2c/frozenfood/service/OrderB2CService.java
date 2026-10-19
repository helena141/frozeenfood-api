package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.dto.request.OrderB2CRequest;
import com.lezzat.b2b2c.frozenfood.dto.request.OrderItemRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.OrderItemResponse;
import com.lezzat.b2b2c.frozenfood.dto.response.OrderResponse;
import com.lezzat.b2b2c.frozenfood.entity.Customer;
import com.lezzat.b2b2c.frozenfood.entity.Order;
import com.lezzat.b2b2c.frozenfood.entity.OrderItem;
import com.lezzat.b2b2c.frozenfood.entity.Product;
import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;
import com.lezzat.b2b2c.frozenfood.exception.InsufficientStockException;
import com.lezzat.b2b2c.frozenfood.repository.CustomerRepository;
import com.lezzat.b2b2c.frozenfood.repository.OrderRepository;
import com.lezzat.b2b2c.frozenfood.repository.ProductBatchRepository;
import com.lezzat.b2b2c.frozenfood.repository.ProductRepository;
import com.lezzat.b2b2c.frozenfood.service.core.FefoInventoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * UC-09: Buat Pesanan Satuan (checkout B2C/eceran).
 *
 * CATATAN ARSITEKTUR PENTING — TECH DEBT YANG DISADARI:
 * FefoInventoryService.deductStockFefo(productId, qty) milik Bony itu void —
 * tidak memberi tahu dari ProductBatch mana saja stok diambil. Padahal
 * OrderItem WAJIB mereferensikan satu ProductBatch spesifik (bukan Product
 * generik), supaya hargaSatuanSnapshot akurat per batch (ProductBatch bisa
 * punya hargaOverride/diskon sendiri, beda dari Product.hargaEceran).
 *
 * Solusi sementara: method ini MEMBACA batch yang tersedia sendiri (query
 * yang SAMA PERSIS dengan yang dipakai FefoInventoryService — urutan
 * stokOnline > 0, ORDER BY tanggalExp ASC) untuk menyusun breakdown
 * OrderItem per-batch, lalu memanggil deductStockFefo() Bony untuk benar-benar
 * mengeksekusi pemotongan stok. Konsekuensinya, LOGIKA URUTAN FEFO SEKARANG
 * ADA DI 2 TEMPAT (di sini, dan di FefoInventoryService) — kalau Bony nanti
 * mengubah urutan/aturan pemilihan batch, breakdown di sini bisa tidak
 * sinkron lagi. Idealnya FefoInventoryService diubah untuk me-return daftar
 * batch yang dipakai, supaya cuma ada 1 sumber kebenaran — ini didiskusikan
 * ke tim dan dicatat sebagai perbaikan lanjutan, bukan diblokir sekarang.
 *
 * ASUMSI lain yang dipakai di sini (tolong dikonfirmasi ke tim):
 * - Harga per item = ProductBatch.hargaOverride kalau ada, kalau tidak pakai
 * Product.hargaEceran, dikurangi ProductBatch.diskon (diasumsikan PERSEN,
 * bukan nominal rupiah).
 * - ongkir diisi 0 saat Order dibuat (placeholder) — dihitung belakangan
 * setelah Delivery dibuat lewat SmartDeliveryService, bukan di langkah ini.
 * - status awal Order B2C: "MENUNGGU_PEMBAYARAN".
 */
@Service
@RequiredArgsConstructor
public class OrderB2CService {

        private static final String TIPE_B2C = "B2C";
        private static final String STATUS_MENUNGGU_PEMBAYARAN = "MENUNGGU_PEMBAYARAN";

        private final OrderRepository orderRepository;
        private final CustomerRepository customerRepository;
        private final ProductRepository productRepository;
        private final ProductBatchRepository productBatchRepository;
        private final FefoInventoryService fefoInventoryService;

        @Transactional
        public OrderResponse createRetailOrder(OrderB2CRequest request) {
                Customer customer = customerRepository.findById(request.idUser())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Customer dengan id " + request.idUser() + " tidak ditemukan"));

                List<OrderItem> orderItems = new ArrayList<>();
                BigDecimal totalTagihan = BigDecimal.ZERO;

                Order order = Order.builder()
                                .customer(customer)
                                .tipeOrder(TIPE_B2C)
                                .status(STATUS_MENUNGGU_PEMBAYARAN)
                                .ongkir(BigDecimal.ZERO)
                                .catatan(request.catatan())
                                .build();

                for (OrderItemRequest itemRequest : request.items()) {
                        List<OrderItem> breakdown = susunBreakdownPerBatch(order, itemRequest);
                        orderItems.addAll(breakdown);

                        for (OrderItem item : breakdown) {
                                totalTagihan = totalTagihan.add(item.getSubtotalItem());
                        }

                        // Eksekusi potong stok yang SEBENARNYA lewat service Bony — ini
                        // satu-satunya tempat yang benar-benar MENGUBAH data stok.
                        fefoInventoryService.deductStockFefo(itemRequest.idProduk(), itemRequest.quantity());
                }

                order.setItems(orderItems);
                order.setTotalTagihan(totalTagihan);

                Order saved = orderRepository.save(order);
                return toResponse(saved);
        }

        /**
         * Membaca batch yang tersedia (urutan FEFO: tanggalExp paling dekat dulu)
         * dan menyusun satu OrderItem per batch yang terpakai, TANPA mengubah
         * stok (baca saja). Stok baru benar-benar dipotong lewat panggilan
         * fefoInventoryService.deductStockFefo() terpisah di createRetailOrder().
         */
        private List<OrderItem> susunBreakdownPerBatch(Order order, OrderItemRequest itemRequest) {
                Product product = productRepository.findById(itemRequest.idProduk())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Produk dengan id " + itemRequest.idProduk() + " tidak ditemukan"));

                List<ProductBatch> availableBatches = productBatchRepository
                                .findByProduct_IdProdukAndStokOnlineGreaterThanOrderByTanggalExpAsc(
                                                itemRequest.idProduk(), 0);

                int totalAvailable = availableBatches.stream()
                                .mapToInt(batch -> batch.getStokOnline() == null ? 0 : batch.getStokOnline())
                                .sum();

                if (totalAvailable < itemRequest.quantity()) {
                        throw new InsufficientStockException(
                                        "Stok tidak mencukupi untuk produk " + product.getNamaProduk()
                                                        + ". Diminta: " + itemRequest.quantity() + ", tersedia: "
                                                        + totalAvailable);
                }

                List<OrderItem> breakdown = new ArrayList<>();
                int sisaDibutuhkan = itemRequest.quantity();

                for (ProductBatch batch : availableBatches) {
                        if (sisaDibutuhkan <= 0) {
                                break;
                        }

                        int diambilDariBatchIni = Math.min(batch.getStokOnline(), sisaDibutuhkan);
                        BigDecimal hargaEfektif = hitungHargaEfektif(product, batch);
                        BigDecimal subtotal = hargaEfektif.multiply(BigDecimal.valueOf(diambilDariBatchIni));

                        breakdown.add(OrderItem.builder()
                                        .order(order)
                                        .productBatch(batch)
                                        .quantity(diambilDariBatchIni)
                                        .hargaSatuanSnapshot(hargaEfektif)
                                        .subtotalItem(subtotal)
                                        .build());

                        sisaDibutuhkan -= diambilDariBatchIni;
                }

                return breakdown;
        }

        private BigDecimal hitungHargaEfektif(Product product, ProductBatch batch) {
                BigDecimal hargaDasar = batch.getHargaOverride() != null
                                ? batch.getHargaOverride()
                                : product.getHargaEceran();

                if (batch.getDiskon() == null || batch.getDiskon().compareTo(BigDecimal.ZERO) == 0) {
                        return hargaDasar;
                }

                // ASUMSI: diskon disimpan sebagai PERSEN (mis. 10.00 = potongan 10%).
                BigDecimal faktorDiskon = BigDecimal.ONE.subtract(
                                batch.getDiskon().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                return hargaDasar.multiply(faktorDiskon).setScale(2, RoundingMode.HALF_UP);
        }

        private OrderResponse toResponse(Order order) {
                List<OrderItemResponse> itemResponses = order.getItems().stream()
                                .map(item -> new OrderItemResponse(
                                                item.getIdOrderItem(),
                                                item.getProductBatch().getProduct().getIdProduk(),
                                                item.getProductBatch().getProduct().getNamaProduk(),
                                                item.getProductBatch().getIdProdBatch(),
                                                item.getQuantity(),
                                                item.getHargaSatuanSnapshot(),
                                                item.getSubtotalItem()))
                                .toList();

                return new OrderResponse(
                                order.getIdOrder(),
                                order.getCustomer().getIdUser(),
                                order.getTipeOrder(),
                                order.getStatus(),
                                order.getOngkir(),
                                order.getTotalTagihan(),
                                order.getCatatan(),
                                itemResponses,
                                order.getCreatedAt());
        }
}

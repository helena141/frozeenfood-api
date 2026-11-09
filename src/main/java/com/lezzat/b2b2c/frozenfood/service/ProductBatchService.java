package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.dto.request.ProductBatchRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.ProductBatchResponse;
import com.lezzat.b2b2c.frozenfood.entity.Product;
import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;
import com.lezzat.b2b2c.frozenfood.repository.ProductBatchRepository;
import com.lezzat.b2b2c.frozenfood.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductBatchService {

    private final ProductBatchRepository productBatchRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ProductBatchResponse createBatch(ProductBatchRequest request) {
        // 1. Cari produk master menggunakan ID String (Contoh: "NGT-001")
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Produk master dengan ID " + request.productId() + " tidak ditemukan"));

        // 2. Generate ID Batch Opsi A (Contoh: "B-NGT001-270515")
        String customBatchId = generateBatchId(product.getIdProduk(), request.tanggalExp());

        // 3. Cek apakah batch dengan produk & tanggal expired yang persis sama sudah ada di gudang
        Optional<ProductBatch> existingBatchOpt = productBatchRepository.findById(customBatchId);

        ProductBatch savedBatch;
        if (existingBatchOpt.isPresent()) {
            // Jika sudah ada, akumulasikan stoknya agar tidak bentrok Primary Key
            ProductBatch existingBatch = existingBatchOpt.get();
            existingBatch.setStokAwal(existingBatch.getStokAwal() + request.stokAwal());
            existingBatch.setStokOnline(existingBatch.getStokOnline() + request.stokAwal());
            if (request.hargaOverride() != null) {
                existingBatch.setHargaOverride(request.hargaOverride());
            }
            if (request.diskon() != null) {
                existingBatch.setDiskon(request.diskon());
            }
            savedBatch = productBatchRepository.save(existingBatch);
        } else {
            // Jika belum ada, buat batch baru dengan customBatchId
            ProductBatch batch = ProductBatch.builder()
                    .idProdBatch(customBatchId) // <-- Pasang ID Batch hasil generate
                    .product(product)
                    .stokAwal(request.stokAwal())
                    .stokOnline(request.stokAwal()) // Saat pertama datang, stok online sama dengan stok fisik awal
                    .tanggalExp(request.tanggalExp())
                    .hargaOverride(request.hargaOverride())
                    .diskon(request.diskon())
                    .build();

            savedBatch = productBatchRepository.save(batch);
        }

        return mapToResponse(savedBatch);
    }

    // Mengambil semua data batch (untuk admin/gudang) — bebas N+1 query dengan JOIN FETCH
    @Transactional(readOnly = true)
    public List<ProductBatchResponse> getAllBatches() {
        return productBatchRepository.findAllWithProductOrderByTanggalExpAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Mengambil data batch dengan filter opsional productId (untuk Dashboard Next.js)
    @Transactional(readOnly = true)
    public List<ProductBatchResponse> getBatches(String productId) {
        List<ProductBatch> batches = (productId != null && !productId.isBlank())
                ? productBatchRepository.findByProductIdWithProductOrderByTanggalExpAsc(productId)
                : productBatchRepository.findAllWithProductOrderByTanggalExpAsc();

        return batches.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Parameter idProduk diubah dari UUID menjadi String
    @Transactional(readOnly = true)
    public List<ProductBatchResponse> getAvailableBatches(String idProduk) {
        return productBatchRepository.findByProduct_IdProdukAndStokOnlineGreaterThanOrderByTanggalExpAsc(idProduk, 0).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // === HELPER 1: GENERATOR ID BATCH OPSI A (B-NGT001-270515) ===
    private String generateBatchId(String idProduk, LocalDate tanggalExp) {
        // Hilangkan tanda strip pada ID Produk ("NGT-001" -> "NGT001")
        String cleanProductCode = idProduk.replace("-", "");
        
        // Format tanggal expired menjadi 6 digit YYMMDD (misal: 2027-05-15 -> "270515")
        String expFormatted = tanggalExp.format(DateTimeFormatter.ofPattern("yyMMdd"));
        
        return "B-" + cleanProductCode + "-" + expFormatted;
    }

    // === HELPER 2: KONVERSI KE RESPONSE DTO ===
    private ProductBatchResponse mapToResponse(ProductBatch batch) {
        return new ProductBatchResponse(
                batch.getIdProdBatch(),
                batch.getProduct().getIdProduk(),
                batch.getProduct().getNamaProduk(),
                batch.getStokAwal(),
                batch.getStokOnline(),
                batch.getTanggalExp(),
                batch.getHargaOverride(),
                batch.getDiskon()
        );
    }
}
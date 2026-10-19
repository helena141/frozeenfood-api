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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductBatchService {

    private final ProductBatchRepository productBatchRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ProductBatchResponse createBatch(ProductBatchRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Produk master dengan ID " + request.productId() + " tidak ditemukan"));

        ProductBatch batch = ProductBatch.builder()
                .product(product)
                .stokAwal(request.stokAwal())
                .stokOnline(request.stokAwal()) // Saat pertama datang, stok online sama dengan stok fisik awal
                .tanggalExp(request.tanggalExp())
                .hargaOverride(request.hargaOverride())
                .diskon(request.diskon())
                .build();

        ProductBatch savedBatch = productBatchRepository.save(batch);

        return mapToResponse(savedBatch);
    }

    // === FITUR TAMPIL DATA BATCH DITAMBAHKAN DI SINI ===

    // Mengambil semua data batch (untuk admin/gudang)
    @Transactional(readOnly = true)
    public List<ProductBatchResponse> getAllBatches() {
        return productBatchRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Logika FEFO: Mengambil batch yang stoknya masih ada untuk produk tertentu
    @Transactional(readOnly = true)
    public List<ProductBatchResponse> getAvailableBatches(UUID idProduk) {
        // Menggunakan nama method dari "KODE BONY" dan mengisi minStok dengan angka 0
        return productBatchRepository.findByProduct_IdProdukAndStokOnlineGreaterThanOrderByTanggalExpAsc(idProduk, 0).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Helper method untuk konversi ke Response biar kodenya tidak berulang
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
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

        return toResponse(savedBatch);
    }

    @Transactional(readOnly = true)
    public List<ProductBatchResponse> getBatches(UUID productId) {
        List<ProductBatch> batches = (productId != null)
                ? productBatchRepository.findByProductIdWithProductOrderByTanggalExpAsc(productId)
                : productBatchRepository.findAllWithProductOrderByTanggalExpAsc();

        return batches.stream()
                .map(this::toResponse)
                .toList();
    }

    private ProductBatchResponse toResponse(ProductBatch batch) {
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
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

        return new ProductBatchResponse(
                savedBatch.getIdProdBatch(),
                savedBatch.getProduct().getIdProduk(),
                savedBatch.getProduct().getNamaProduk(),
                savedBatch.getStokAwal(),
                savedBatch.getStokOnline(),
                savedBatch.getTanggalExp(),
                savedBatch.getHargaOverride(),
                savedBatch.getDiskon()
        );
    }
}
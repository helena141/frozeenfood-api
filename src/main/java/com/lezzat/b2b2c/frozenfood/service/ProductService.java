package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.dto.request.ProductRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.ProductCatalogResponse;
import com.lezzat.b2b2c.frozenfood.entity.Product;
import com.lezzat.b2b2c.frozenfood.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductCatalogResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductCatalogResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
        return mapToResponse(product);
    }

    @Transactional
    public ProductCatalogResponse createProduct(ProductRequest request) {
        Product product = new Product();
        // Menggunakan setter sesuai nama variabel bahasa Indonesia dari temanmu
        product.setNamaProduk(request.getName());
        product.setDeskripsiProduk(request.getDescription());
        product.setHargaEceran(request.getPrice());
        product.setKategori(request.getCategory());
        // Tambahan default wajib agar tidak error saat di-save
        product.setBerat(java.math.BigDecimal.ZERO); 
        
        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Transactional
    public ProductCatalogResponse updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
        
        // Menggunakan setter sesuai nama variabel bahasa Indonesia
        product.setNamaProduk(request.getName());
        product.setDeskripsiProduk(request.getDescription());
        product.setHargaEceran(request.getPrice());
        product.setKategori(request.getCategory());
        
        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        productRepository.deleteById(id);
    }

   // Helper method untuk map dari Entity (Bahasa Indonesia) ke DTO (Bahasa Inggris)
    private ProductCatalogResponse mapToResponse(Product product) {
        int totalAvailableStock = 0;
        
        // Kalkulasi stok dari relasi @OneToMany ProductBatch
        if (product.getProductBatches() != null) {
            // Kita jumlahkan semua stok_online dari batch yang terhubung ke produk ini
            totalAvailableStock = product.getProductBatches().stream()
                    .mapToInt(batch -> batch.getStokOnline() != null ? batch.getStokOnline() : 0)
                    .sum();
        }

        return ProductCatalogResponse.builder()
                .id(product.getIdProduk())
                .name(product.getNamaProduk())
                .description(product.getDeskripsiProduk())
                .price(product.getHargaEceran())
                .category(product.getKategori())
                .totalStock(totalAvailableStock) // Sekarang stok di Katalog akan menampilkan angka real!
                .build();
    }
}
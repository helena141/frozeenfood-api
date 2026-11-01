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

    @Transactional
    public ProductCatalogResponse createProduct(ProductRequest request) {
        // 1. Mapping dari DTO ke Entity Product
        Product product = Product.builder()
                .namaProduk(request.getNamaProduk())
                .deskripsiProduk(request.getDeskripsiProduk())
                .kategori(request.getKategori())
                .hargaEceran(request.getHargaEceran())
                .berat(request.getBerat())
                .volume(request.getVolume())
                .gambar(request.getGambar())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        // 2. Simpan ke database
        Product savedProduct = productRepository.save(product);

        // 3. Convert ke Response DTO
        return mapToResponse(savedProduct);
    }

    @Transactional
    public ProductCatalogResponse updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan dengan ID: " + id));

        // Update nilai field
        product.setNamaProduk(request.getNamaProduk());
        product.setDeskripsiProduk(request.getDeskripsiProduk());
        product.setKategori(request.getKategori());
        product.setHargaEceran(request.getHargaEceran());
        product.setBerat(request.getBerat());
        product.setVolume(request.getVolume());
        product.setGambar(request.getGambar());
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }

        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    @Transactional(readOnly = true)
    public List<ProductCatalogResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // === FITUR SEARCH DITAMBAHKAN DI SINI ===
    @Transactional(readOnly = true)
    public List<ProductCatalogResponse> searchProducts(String keyword) {
        return productRepository.findByNamaProdukContainingIgnoreCase(keyword, keyword).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    // ========================================

    @Transactional(readOnly = true)
    public ProductCatalogResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan dengan ID: " + id));
        return mapToResponse(product);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Produk tidak ditemukan dengan ID: " + id);
        }
        // TODO: (Sprint Berikutnya) Tambahkan validasi cek histori ke tabel order_details. 
        // Jika produk sudah pernah terjual, batalkan delete dan minta user melakukan Deactivate (isActive = false).
        productRepository.deleteById(id);
    }

    // Helper method untuk konversi Entity ke Response DTO
    private ProductCatalogResponse mapToResponse(Product product) {
        return ProductCatalogResponse.builder()
                .idProduk(product.getIdProduk())
                .namaProduk(product.getNamaProduk())
                .deskripsiProduk(product.getDeskripsiProduk())
                .kategori(product.getKategori())
                .hargaEceran(product.getHargaEceran())
                .berat(product.getBerat())
                .volume(product.getVolume())
                .gambar(product.getGambar())
                .isActive(product.getIsActive())
                .totalStock(0)
                .build();
    }
}
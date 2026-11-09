package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.dto.request.ProductRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.ProductCatalogResponse;
import com.lezzat.b2b2c.frozenfood.entity.Product;
import com.lezzat.b2b2c.frozenfood.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductCatalogResponse createProduct(ProductRequest request) {
        
        // 1. Validasi nama produk ganda (Cegah Duplikat)
        if (productRepository.existsByNamaProdukIgnoreCase(request.getNamaProduk())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "Oops! Produk '" + request.getNamaProduk() + "' sepertinya sudah ada di daftar jualanmu nih. Mau edit produk yang sudah ada saja?");
        }

        // 2. Generate Custom ID Produk berdasarkan Kategori (Contoh: "NGT-001")
        String customId = generateProductId(request.getKategori());

        // 3. Mapping dari DTO ke Entity Product
        Product product = Product.builder()
                .idProduk(customId)
                .namaProduk(request.getNamaProduk())
                .deskripsiProduk(request.getDeskripsiProduk())
                .kategori(request.getKategori())
                .hargaEceran(request.getHargaEceran())
                .berat(request.getBerat())
                .volume(request.getVolume())
                .gambar(request.getGambar())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        // 4. Simpan ke database
        Product savedProduct = productRepository.save(product);

        // 5. Convert ke Response DTO
        return mapToResponse(savedProduct);
    }

    @Transactional
    public ProductCatalogResponse updateProduct(String id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan dengan ID: " + id));

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

    @Transactional(readOnly = true)
    public List<ProductCatalogResponse> searchProducts(String keyword) {
        return productRepository.findByNamaProdukContainingIgnoreCaseOrKategoriContainingIgnoreCase(keyword, keyword).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductCatalogResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan dengan ID: " + id));
        return mapToResponse(product);
    }

    @Transactional
    public void deleteProduct(String id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Produk tidak ditemukan dengan ID: " + id);
        }
        productRepository.deleteById(id);
    }

    // === HELPER 1: GENERATOR CUSTOM ID PRODUK (NGT-001, BKS-001, dst) ===
    private String generateProductId(String kategori) {
        String prefix;
        if (kategori == null || kategori.isBlank()) {
            prefix = "PRD";
        } else {
            String cleanKategori = kategori.trim().toLowerCase();
            if (cleanKategori.contains("nugget")) {
                prefix = "NGT";
            } else if (cleanKategori.contains("bakso")) {
                prefix = "BKS";
            } else if (cleanKategori.contains("sosis")) {
                prefix = "SOS";
            } else if (cleanKategori.contains("kentang")) {
                prefix = "KTG";
            } else if (cleanKategori.contains("dimsum")) {
                prefix = "DMS";
            } else if (cleanKategori.length() >= 3) {
                prefix = cleanKategori.substring(0, 3).toUpperCase();
            } else {
                prefix = "PRD";
            }
        }

        Optional<Product> lastProductOpt = productRepository.findTopByIdProdukStartingWithOrderByIdProdukDesc(prefix + "-");
        int nextNumber = 1;
        if (lastProductOpt.isPresent()) {
            String lastId = lastProductOpt.get().getIdProduk();
            String[] parts = lastId.split("-");
            if (parts.length > 1) {
                try {
                    nextNumber = Integer.parseInt(parts[1]) + 1;
                } catch (NumberFormatException ignored) {
                    nextNumber = 1;
                }
            }
        }

        return String.format("%s-%03d", prefix, nextNumber);
    }

    // === HELPER 2: KONVERSI ENTITY KE RESPONSE DTO ===
    private ProductCatalogResponse mapToResponse(Product product) {
        int totalAvailableStock = 0;
        if (product.getProductBatches() != null) {
            totalAvailableStock = product.getProductBatches().stream()
                    .mapToInt(batch -> batch.getStokOnline() != null ? batch.getStokOnline() : 0)
                    .sum();
        }

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
                .totalStock(totalAvailableStock)
                .build();
    }
}
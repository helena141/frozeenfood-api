package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.dto.request.PaymentCallbackRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.ProductResponse;
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

/**
 * Service CRUD Produk (jalur B2C).
 * Product TIDAK punya relasi @OneToMany ke ProductBatch (relasinya
 * satu arah: ProductBatch -> Product), jadi total stok online dihitung
 * lewat query terpisah ke ProductBatchRepository.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductBatchRepository productBatchRepository;

    @Transactional
    public ProductResponse createProduct(PaymentCallbackRequest request) {
        Product product = Product.builder()
                .namaProduk(request.namaProduk())
                .deskripsiProduk(request.deskripsiProduk())
                .kategori(request.kategori())
                .hargaEceran(request.hargaEceran())
                .berat(request.berat())
                .volume(request.volume())
                .gambar(request.gambar())
                .isActive(request.isActive() == null ? Boolean.TRUE : request.isActive())
                .build();

        Product saved = productRepository.save(product);
        // Produk baru belum mungkin punya batch stok.
        return toResponseDTO(saved, 0);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        // CATATAN PERFORMA: ini query N+1 (1x findAll + Nx hitung stok per produk).
        // Cukup untuk tahap ini; kalau perlu dioptimasi nanti, minta Bony buatkan
        // query agregat (GROUP BY id_produk) di ProductBatchRepository.
        return productRepository.findAll()
                .stream()
                .map(product -> toResponseDTO(product, hitungTotalStokOnline(product.getIdProduk())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID idProduk) {
        Product product = findProductOrThrow(idProduk);
        return toResponseDTO(product, hitungTotalStokOnline(idProduk));
    }

    @Transactional
    public ProductResponse updateProduct(UUID idProduk, PaymentCallbackRequest request) {
        Product product = findProductOrThrow(idProduk);

        product.setNamaProduk(request.namaProduk());
        product.setDeskripsiProduk(request.deskripsiProduk());
        product.setKategori(request.kategori());
        product.setHargaEceran(request.hargaEceran());
        product.setBerat(request.berat());
        product.setVolume(request.volume());
        product.setGambar(request.gambar());
        if (request.isActive() != null) {
            product.setIsActive(request.isActive());
        }

        return toResponseDTO(product, hitungTotalStokOnline(idProduk));
    }

    @Transactional
    public void deleteProduct(UUID idProduk) {
        Product product = findProductOrThrow(idProduk);
        // @SQLDelete pada entity Product mengubah delete() ini menjadi
        // UPDATE ... SET deleted_at = now(), bukan hard delete.
        productRepository.delete(product);
    }

    private Product findProductOrThrow(UUID idProduk) {
        return productRepository.findById(idProduk)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Produk dengan id " + idProduk + " tidak ditemukan"));
    }

    private int hitungTotalStokOnline(UUID idProduk) {
        // ASUMSI: method ini perlu ditambahkan Bony di ProductBatchRepository:
        // List<ProductBatch> findByProduct_IdProduk(UUID idProduk);
        List<ProductBatch> batches = productBatchRepository.findByProduct_IdProduk(idProduk);
        return batches.stream()
                .mapToInt(batch -> batch.getStokOnline() == null ? 0 : batch.getStokOnline())
                .sum();
    }

    private ProductResponse toResponseDTO(Product product, int totalStokOnline) {
        return new ProductResponse(
                product.getIdProduk(),
                product.getNamaProduk(),
                product.getDeskripsiProduk(),
                product.getKategori(),
                product.getHargaEceran(),
                product.getBerat(),
                product.getVolume(),
                product.getGambar(),
                product.getIsActive(),
                totalStokOnline,
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}

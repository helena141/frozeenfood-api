package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.dto.request.ProductRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.ProductCatalogResponse;
import com.lezzat.b2b2c.frozenfood.entity.Product;
import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;
import com.lezzat.b2b2c.frozenfood.entity.TieringPrice;
import com.lezzat.b2b2c.frozenfood.repository.ProductRepository;
import com.lezzat.b2b2c.frozenfood.repository.TieringPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String UPLOAD_DIR = "uploads/products/";
    private static final int CLEARANCE_DAYS_THRESHOLD = 7;
    private static final BigDecimal CLEARANCE_MULTIPLIER = new BigDecimal("0.50");

    private final ProductRepository productRepository;
    private final TieringPriceRepository tieringPriceRepository;

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

    @Transactional
    public ProductCatalogResponse addTieringPrice(String idProduk, Integer minQty, Integer maxQty, BigDecimal hargaPerUnit) {
        Product product = productRepository.findById(idProduk)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan dengan ID: " + idProduk));
        TieringPrice tiering = TieringPrice.builder()
                .product(product)
                .minQty(minQty)
                .maxQty(maxQty)
                .hargaPerUnit(hargaPerUnit)
                .build();
        tieringPriceRepository.save(tiering);
        return mapToResponse(product);
    }

    @Transactional
    public void deleteTieringPrice(UUID idTiering) {
        if (!tieringPriceRepository.existsById(idTiering)) {
            throw new RuntimeException("TieringPrice tidak ditemukan dengan ID: " + idTiering);
        }
        tieringPriceRepository.deleteById(idTiering);
    }

    @Transactional
    public ProductCatalogResponse uploadProductImage(String idProduk, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File gambar tidak boleh kosong.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("File harus berupa gambar (JPG, PNG, atau WEBP).");
        }
        Product product = productRepository.findById(idProduk)
                .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan dengan ID: " + idProduk));
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String fileName = product.getIdProduk() + "_" + System.currentTimeMillis() + extension;
            Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            product.setGambar("/" + UPLOAD_DIR + fileName);
            return mapToResponse(productRepository.save(product));
        } catch (IOException e) {
            throw new RuntimeException("Gagal menyimpan gambar produk: " + e.getMessage());
        }
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
        int stokClearance = 0;
        LocalDate nearestExpDate = null;
        BigDecimal hargaClearance = null;
        LocalDate clearanceCutoff = LocalDate.now().plusDays(CLEARANCE_DAYS_THRESHOLD);

        if (product.getProductBatches() != null) {
            List<ProductBatch> activeBatches = product.getProductBatches().stream()
                    .filter(b -> b.getStokOnline() != null && b.getStokOnline() > 0)
                    .sorted(Comparator.comparing(ProductBatch::getTanggalExp))
                    .toList();

            for (ProductBatch batch : activeBatches) {
                totalAvailableStock += batch.getStokOnline();
                if (nearestExpDate == null) {
                    nearestExpDate = batch.getTanggalExp();
                }
                if (!batch.getTanggalExp().isAfter(clearanceCutoff)) {
                    stokClearance += batch.getStokOnline();
                    if (hargaClearance == null) {
                        hargaClearance = batch.getHargaOverride() != null
                                ? batch.getHargaOverride()
                                : product.getHargaEceran().multiply(CLEARANCE_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
                    }
                }
            }
        }

        List<ProductCatalogResponse.TieringPriceItem> tieringItems = tieringPriceRepository
                .findByProduct_IdProdukOrderByMinQtyAsc(product.getIdProduk()).stream()
                .map(t -> new ProductCatalogResponse.TieringPriceItem(
                        t.getIdTiering(), t.getMinQty(), t.getMaxQty(), t.getHargaPerUnit()))
                .toList();

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
                .isClearance(stokClearance > 0)
                .stokClearance(stokClearance)
                .hargaClearance(hargaClearance)
                .nearestExpDate(nearestExpDate)
                .tieringPrices(tieringItems)
                .build();
    }
}
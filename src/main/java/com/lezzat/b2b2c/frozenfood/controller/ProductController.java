package com.lezzat.b2b2c.frozenfood.controller;

import com.lezzat.b2b2c.frozenfood.dto.request.ProductRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.ProductCatalogResponse;
import com.lezzat.b2b2c.frozenfood.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductCatalogResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // === FITUR SEARCH ===
    @GetMapping("/search")
    public ResponseEntity<List<ProductCatalogResponse>> searchProducts(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    // Parameter @PathVariable diubah dari UUID menjadi String (contoh: /api/products/NGT-001)
    @GetMapping("/{id}")
    public ResponseEntity<ProductCatalogResponse> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<ProductCatalogResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return new ResponseEntity<>(productService.createProduct(request), HttpStatus.CREATED);
    }

    // Parameter @PathVariable diubah dari UUID menjadi String
    @PutMapping("/{id}")
    public ResponseEntity<ProductCatalogResponse> updateProduct(
            @PathVariable String id, 
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    // Parameter @PathVariable diubah dari UUID menjadi String
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    public record TieringPriceRequest(Integer minQty, Integer maxQty, BigDecimal hargaPerUnit) {}

    @PostMapping("/{id}/tiering")
    public ResponseEntity<ProductCatalogResponse> addTieringPrice(
            @PathVariable String id,
            @RequestBody TieringPriceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addTieringPrice(id, request.minQty(), request.maxQty(), request.hargaPerUnit()));
    }

    @DeleteMapping("/tiering/{idTiering}")
    public ResponseEntity<Void> deleteTieringPrice(@PathVariable UUID idTiering) {
        productService.deleteTieringPrice(idTiering);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductCatalogResponse> uploadProductImage(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productService.uploadProductImage(id, file));
    }
}
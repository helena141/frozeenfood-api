package com.lezzat.b2b2c.frozenfood.controller;

import com.lezzat.b2b2c.frozenfood.dto.request.ProductRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.ProductCatalogResponse;
import com.lezzat.b2b2c.frozenfood.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
    public ResponseEntity<ProductCatalogResponse> createProduct(@RequestBody ProductRequest request) {
        return new ResponseEntity<>(productService.createProduct(request), HttpStatus.CREATED);
    }

    // Parameter @PathVariable diubah dari UUID menjadi String
    @PutMapping("/{id}")
    public ResponseEntity<ProductCatalogResponse> updateProduct(
            @PathVariable String id, 
            @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    // Parameter @PathVariable diubah dari UUID menjadi String
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
package com.lezzat.b2b2c.frozenfood.controller;

import com.lezzat.b2b2c.frozenfood.dto.request.PaymentCallbackRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.ProductResponse;
import com.lezzat.b2b2c.frozenfood.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Controller resource-based ("/api/products"), bukan actor-based,
// sesuai hukum arsitektur nomor 5.
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody PaymentCallbackRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentCallbackRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}

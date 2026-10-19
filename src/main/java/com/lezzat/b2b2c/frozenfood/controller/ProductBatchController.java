package com.lezzat.b2b2c.frozenfood.controller;

import com.lezzat.b2b2c.frozenfood.dto.request.ProductBatchRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.ProductBatchResponse;
import com.lezzat.b2b2c.frozenfood.service.ProductBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/batches")
@RequiredArgsConstructor
public class ProductBatchController {

    private final ProductBatchService productBatchService;

    // POST: Tambah stok batch baru
    @PostMapping
    public ResponseEntity<ProductBatchResponse> createBatch(@Valid @RequestBody ProductBatchRequest request) {
        ProductBatchResponse response = productBatchService.createBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // === TAMBAHAN DUA METHOD GET UNTUK FEFO ===

    // GET: Lihat semua riwayat batch di sistem (Untuk Admin)
    @GetMapping
    public ResponseEntity<List<ProductBatchResponse>> getAllBatches() {
        return ResponseEntity.ok(productBatchService.getAllBatches());
    }

    // GET: Lihat stok yang tersedia per produk (Logika FEFO)
    @GetMapping("/product/{idProduk}")
    public ResponseEntity<List<ProductBatchResponse>> getAvailableBatches(@PathVariable UUID idProduk) {
        return ResponseEntity.ok(productBatchService.getAvailableBatches(idProduk));
    }
}
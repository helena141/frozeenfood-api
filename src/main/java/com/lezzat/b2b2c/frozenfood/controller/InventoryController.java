package com.lezzat.b2b2c.frozenfood.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lezzat.b2b2c.frozenfood.service.core.FefoInventoryService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final FefoInventoryService fefoInventoryService;

    public InventoryController(FefoInventoryService fefoInventoryService) {
        this.fefoInventoryService = fefoInventoryService;
    }

    /**
     * Endpoint untuk mengurangi stok dengan algoritma FEFO
     * Contoh Request: POST /api/inventory/deduct?productId=123e4567-e89b-12d3-a456-426614174000&quantity=5
     */
    @PostMapping("/deduct")
    public ResponseEntity<Map<String, String>> deductStock(
            @RequestParam UUID productId,
            @RequestParam Integer quantity) {
        
        try {
            fefoInventoryService.deductStockFefo(productId, quantity);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Stok berhasil dikurangi menggunakan metode FEFO"
            ));
        } catch (RuntimeException e) {
            // Menangkap error jika stok tidak cukup, lalu mengembalikan status 400 (Bad Request)
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}
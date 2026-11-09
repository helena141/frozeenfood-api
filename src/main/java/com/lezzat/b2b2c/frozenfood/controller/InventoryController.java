package com.lezzat.b2b2c.frozenfood.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lezzat.b2b2c.frozenfood.service.core.FefoInventoryService;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final FefoInventoryService fefoInventoryService;

    public InventoryController(FefoInventoryService fefoInventoryService) {
        this.fefoInventoryService = fefoInventoryService;
    }

    @PostMapping("/deduct")
    public ResponseEntity< Map< String, String > > deductStock(
            @RequestParam String productId,
            @RequestParam Integer quantity) {
        
        try {
            fefoInventoryService.deductStockFefo(productId, quantity);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Stok berhasil dikurangi menggunakan metode FEFO"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}
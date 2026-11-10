package com.lezzat.b2b2c.frozenfood.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.lezzat.b2b2c.frozenfood.entity.ExpiryAlert;
import com.lezzat.b2b2c.frozenfood.repository.ExpiryAlertRepository;
import com.lezzat.b2b2c.frozenfood.service.core.FefoInventoryService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final FefoInventoryService fefoInventoryService;
    private final ExpiryAlertRepository expiryAlertRepository;

    public InventoryController(FefoInventoryService fefoInventoryService,
                               ExpiryAlertRepository expiryAlertRepository) {
        this.fefoInventoryService = fefoInventoryService;
        this.expiryAlertRepository = expiryAlertRepository;
    }

    public record ExpiryAlertResponse(
            UUID idAlert,
            String idProdBatch,
            String idProduk,
            String namaProduk,
            LocalDate tanggalExp,
            Integer stokOnline,
            String pesan,
            Boolean isResolved,
            LocalDateTime createdAt
    ) {}

    @GetMapping("/alerts")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ExpiryAlertResponse>> getAlerts(
            @RequestParam(defaultValue = "false") boolean unresolvedOnly) {
        List<ExpiryAlertResponse> result = expiryAlertRepository.findAllWithBatchAndProduct().stream()
                .filter(a -> !unresolvedOnly || !Boolean.TRUE.equals(a.getIsResolved()))
                .map(a -> new ExpiryAlertResponse(
                        a.getIdAlert(),
                        a.getProductBatch().getIdProdBatch(),
                        a.getProductBatch().getProduct().getIdProduk(),
                        a.getProductBatch().getProduct().getNamaProduk(),
                        a.getProductBatch().getTanggalExp(),
                        a.getProductBatch().getStokOnline(),
                        a.getPesan(),
                        a.getIsResolved(),
                        a.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/alerts/{idAlert}/resolve")
    @Transactional
    public ResponseEntity<Map<String, Object>> resolveAlert(@PathVariable UUID idAlert) {
        ExpiryAlert alert = expiryAlertRepository.findById(idAlert)
                .orElseThrow(() -> new EntityNotFoundException("ExpiryAlert dengan ID " + idAlert + " tidak ditemukan"));
        alert.setIsResolved(true);
        expiryAlertRepository.save(alert);
        return ResponseEntity.ok(Map.of("idAlert", idAlert, "isResolved", true));
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
package com.lezzat.b2b2c.frozenfood.controller;

import com.lezzat.b2b2c.frozenfood.dto.request.DeliveryEstimateRequest;
import com.lezzat.b2b2c.frozenfood.dto.request.DeliveryKonfirmasiRequest;
import com.lezzat.b2b2c.frozenfood.dto.request.DeliveryPengantarRequest;
import com.lezzat.b2b2c.frozenfood.dto.request.DeliveryRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.DeliveryEstimateResponse;
import com.lezzat.b2b2c.frozenfood.dto.response.DeliveryResponse;
import com.lezzat.b2b2c.frozenfood.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    public ResponseEntity<List<DeliveryResponse>> getAll(
            @RequestParam(required = false) String statusDelivery,
            @RequestParam(required = false) String status) {
        String filter = (statusDelivery != null && !statusDelivery.isBlank()) ? statusDelivery : status;
        return ResponseEntity.ok(deliveryService.getAllDeliveries(filter));
    }

    @PostMapping
    public ResponseEntity<DeliveryResponse> create(@Valid @RequestBody DeliveryRequest request) {
        DeliveryResponse response = deliveryService.createDelivery(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Pratinjau ongkir/jadwal/pelaksana tanpa menyimpan apa pun. */
    @PostMapping("/estimate")
    public ResponseEntity<DeliveryEstimateResponse> estimate(@Valid @RequestBody DeliveryEstimateRequest request) {
        return ResponseEntity.ok(deliveryService.estimate(request));
    }

    @PatchMapping("/{id}/konfirmasi-admin")
    public ResponseEntity<DeliveryResponse> konfirmasiAdmin(
            @PathVariable UUID id, @Valid @RequestBody DeliveryKonfirmasiRequest request) {
        return ResponseEntity.ok(deliveryService.konfirmasiAdmin(id, request));
    }

    @PatchMapping("/{id}/pengantar")
    public ResponseEntity<DeliveryResponse> setPengantar(
            @PathVariable UUID id, @Valid @RequestBody DeliveryPengantarRequest request) {
        return ResponseEntity.ok(deliveryService.setPengantar(id, request.namaPengantar()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryService.getById(id));
    }

    @GetMapping("/order/{idOrder}")
    public ResponseEntity<DeliveryResponse> getByOrder(@PathVariable UUID idOrder) {
        return ResponseEntity.ok(deliveryService.getByOrderId(idOrder));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DeliveryResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam String statusDelivery) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, statusDelivery));
    }
}

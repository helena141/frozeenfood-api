package com.lezzat.b2b2c.frozenfood.controller;

import com.lezzat.b2b2c.frozenfood.dto.request.DeliveryRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.DeliveryResponse;
import com.lezzat.b2b2c.frozenfood.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    public ResponseEntity<DeliveryResponse> create(@Valid @RequestBody DeliveryRequest request) {
        DeliveryResponse response = deliveryService.createDelivery(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

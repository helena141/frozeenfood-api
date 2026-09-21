package com.lezzat.b2b2c.frozenfood.controller;

import com.lezzat.b2b2c.frozenfood.dto.request.PaymentCallbackRequest;
import com.lezzat.b2b2c.frozenfood.dto.request.PaymentRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.PaymentResponse;
import com.lezzat.b2b2c.frozenfood.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> initiate(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Endpoint publik untuk dipanggil payment gateway (webhook), bukan oleh customer langsung.
    @PostMapping("/callback")
    public ResponseEntity<PaymentResponse> callback(@Valid @RequestBody PaymentCallbackRequest request) {
        return ResponseEntity.ok(paymentService.handleCallback(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @GetMapping("/order/{idOrder}")
    public ResponseEntity<List<PaymentResponse>> getByOrder(@PathVariable UUID idOrder) {
        return ResponseEntity.ok(paymentService.getByOrderId(idOrder));
    }
}

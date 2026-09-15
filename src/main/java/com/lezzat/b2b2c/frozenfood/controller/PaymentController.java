package com.lezzat.b2b2c.frozenfood.controller;

import com.lezzat.b2b2c.frozenfood.dto.request.PaymentCallbackRequest;
import com.lezzat.b2b2c.frozenfood.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/callback")
    public ResponseEntity<String> handlePaymentCallback(@Valid @RequestBody PaymentCallbackRequest request) {
        paymentService.processPaymentCallback(request);
        return ResponseEntity.ok("Payment callback successfully processed");
    }
}
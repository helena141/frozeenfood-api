package com.lezzat.b2b2c.frozenfood.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentCallbackRequest(
        @NotBlank(message = "Gateway Reference is required")
        String gatewayRef,
        
        @NotBlank(message = "Payment status is required")
        String status, // Contoh ekspektasi: "SUCCESS", "FAILED"
        
        @NotNull(message = "Amount is required")
        BigDecimal jumlah
) {}
package com.lezzat.b2b2c.frozenfood.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID idPayment,
        UUID idOrder,
        String gatewayRef,
        String metodePay,
        BigDecimal jumlah,
        String status,
        LocalDateTime paidAt,
        LocalDateTime expiredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

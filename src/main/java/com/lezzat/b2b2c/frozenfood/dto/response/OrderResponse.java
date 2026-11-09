package com.lezzat.b2b2c.frozenfood.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID idOrder,
        UUID idUser,
        String tipeOrder,
        String status,
        BigDecimal ongkir,
        BigDecimal totalTagihan,
        String catatan,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {
}

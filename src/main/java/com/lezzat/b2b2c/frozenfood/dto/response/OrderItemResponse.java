package com.lezzat.b2b2c.frozenfood.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID idOrderItem,
        UUID idProduk,
        String namaProduk,
        UUID idProdBatch,
        Integer quantity,
        BigDecimal hargaSatuanSnapshot,
        BigDecimal subtotalItem
) {
}

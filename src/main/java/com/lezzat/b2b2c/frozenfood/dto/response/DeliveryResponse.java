package com.lezzat.b2b2c.frozenfood.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryResponse(
        UUID idDelivery,
        UUID idOrder,
        String alamatTujuan,
        Float jarak,
        String metodeDeliv,
        String statusDelivery,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String pelaksana,
        BigDecimal ongkir,
        String sumberJarak,
        LocalDateTime estimasiTiba,
        String namaPengantar,
        String alasanKeputusan
) {
}

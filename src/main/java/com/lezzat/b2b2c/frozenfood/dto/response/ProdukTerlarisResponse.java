package com.lezzat.b2b2c.frozenfood.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProdukTerlarisResponse(
        UUID idProduk,
        String namaProduk,
        Integer totalUnitTerjual,
        BigDecimal totalPenjualan
) {
}

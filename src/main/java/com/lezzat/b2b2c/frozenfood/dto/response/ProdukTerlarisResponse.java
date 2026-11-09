package com.lezzat.b2b2c.frozenfood.dto.response;

import java.math.BigDecimal;

public record ProdukTerlarisResponse(
        String idProduk,
        String namaProduk,
        Integer totalUnitTerjual,
        BigDecimal totalPenjualan
) {
}

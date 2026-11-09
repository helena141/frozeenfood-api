package com.lezzat.b2b2c.frozenfood.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductBatchResponse(
        String idProdBatch,
        String productId,
        String namaProduk,
        Integer stokAwal,
        Integer stokOnline,
        LocalDate tanggalExp,
        BigDecimal hargaOverride,
        BigDecimal diskon
) {
}
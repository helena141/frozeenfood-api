package com.lezzat.b2b2c.frozenfood.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductBatchResponse(
        UUID idProdBatch,
        UUID productId,
        String namaProduk,
        Integer stokAwal,
        Integer stokOnline,
        LocalDate tanggalExp,
        BigDecimal hargaOverride,
        BigDecimal diskon
) {
}
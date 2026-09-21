package com.lezzat.b2b2c.frozenfood.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportResponse(
        LocalDate periodeMulai,
        LocalDate periodeAkhir,
        BigDecimal totalOmzet,
        Long jumlahTransaksiLunas,
        BigDecimal rataRataTransaksi,
        BigDecimal totalOmzetB2C,
        BigDecimal totalOmzetB2B
) {
}

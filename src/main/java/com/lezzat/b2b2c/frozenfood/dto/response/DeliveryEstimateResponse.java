package com.lezzat.b2b2c.frozenfood.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DeliveryEstimateResponse(
        BigDecimal jarakKm,
        String sumberJarak,
        int jarakDibulatkanKm,
        String pelaksana,
        boolean perluKonfirmasiAdmin,
        BigDecimal ongkir,
        boolean ongkirBersifatEstimasi,
        boolean jadwalHariIni,
        LocalDateTime estimasiTiba,
        int menitPerjalanan,
        BigDecimal beratMuatanKg,
        BigDecimal volumeMuatanLiter,
        List<String> alasan,
        List<String> peringatan
) {
}

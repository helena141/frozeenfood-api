package com.lezzat.b2b2c.frozenfood.dto.response;

import java.time.LocalDate;
import java.util.List;

public record LaporanPenjualanResponse(
        LocalDate periodeMulai,
        LocalDate periodeAkhir,
        Long jumlahPesanan,
        Integer totalUnitTerjual,
        Long jumlahPesananB2C,
        Long jumlahPesananB2B,
        Integer unitTerjualB2C,
        Integer unitTerjualB2B,
        List<ProdukTerlarisResponse> produkTerlaris
) {
}

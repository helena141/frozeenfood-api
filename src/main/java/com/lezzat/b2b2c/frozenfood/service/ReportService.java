package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.dto.response.ReportResponse;
import com.lezzat.b2b2c.frozenfood.entity.Payment;
import com.lezzat.b2b2c.frozenfood.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ASUMSI: omzet dihitung dari Payment berstatus LUNAS (uang yang benar-benar
 * masuk), bukan dari Order.totalTagihan (yang cuma tagihan, belum tentu
 * lunas). Order di sini dibaca lewat relasi Payment.order (LAZY) untuk
 * mengetahui tipeOrder (B2C/B2B) — tidak pernah mengubah data Order.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String STATUS_LUNAS = "LUNAS";
    private static final String TIPE_B2C = "B2C";
    private static final String TIPE_B2B = "B2B";

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public ReportResponse getLaporanOmzet(LocalDate periodeMulai, LocalDate periodeAkhir) {
        LocalDateTime awal = periodeMulai.atStartOfDay();
        LocalDateTime akhir = periodeAkhir.atTime(23, 59, 59);

        List<Payment> pembayaranLunas =
                paymentRepository.findByStatusAndPaidAtBetween(STATUS_LUNAS, awal, akhir);

        BigDecimal totalOmzet = BigDecimal.ZERO;
        BigDecimal totalOmzetB2C = BigDecimal.ZERO;
        BigDecimal totalOmzetB2B = BigDecimal.ZERO;

        for (Payment payment : pembayaranLunas) {
            BigDecimal jumlah = payment.getJumlah();
            totalOmzet = totalOmzet.add(jumlah);

            String tipeOrder = payment.getOrder().getTipeOrder();
            if (TIPE_B2C.equalsIgnoreCase(tipeOrder)) {
                totalOmzetB2C = totalOmzetB2C.add(jumlah);
            } else if (TIPE_B2B.equalsIgnoreCase(tipeOrder)) {
                totalOmzetB2B = totalOmzetB2B.add(jumlah);
            }
        }

        long jumlahTransaksi = pembayaranLunas.size();
        BigDecimal rataRata = jumlahTransaksi == 0
                ? BigDecimal.ZERO
                : totalOmzet.divide(BigDecimal.valueOf(jumlahTransaksi), 2, RoundingMode.HALF_UP);

        return new ReportResponse(
                periodeMulai,
                periodeAkhir,
                totalOmzet,
                jumlahTransaksi,
                rataRata,
                totalOmzetB2C,
                totalOmzetB2B
        );
    }
}

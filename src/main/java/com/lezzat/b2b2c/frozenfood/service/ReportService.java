package com.lezzat.b2b2c.frozenfood.service;

import com.lezzat.b2b2c.frozenfood.dto.response.LaporanPenjualanResponse;
import com.lezzat.b2b2c.frozenfood.dto.response.ProdukTerlarisResponse;
import com.lezzat.b2b2c.frozenfood.dto.response.ReportResponse;
import com.lezzat.b2b2c.frozenfood.entity.OrderItem;
import com.lezzat.b2b2c.frozenfood.entity.Payment;
import com.lezzat.b2b2c.frozenfood.repository.OrderItemRepository;
import com.lezzat.b2b2c.frozenfood.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dua laporan dengan sumber data BEDA, sengaja dipisah sesuai UC-31 vs UC-32:
 * - getLaporanOmzet() = UC-32 Laporan Keuangan: dari Payment berstatus LUNAS
 *   (uang yang benar-benar masuk), bukan dari Order.totalTagihan (yang cuma
 *   tagihan, belum tentu lunas).
 * - getLaporanPenjualan() = UC-31 Laporan Penjualan: dari OrderItem (volume
 *   pesanan & unit terjual), TIDAK bergantung pada status pembayaran — order
 *   yang belum dibayar pun tetap terhitung sebagai "penjualan" di sini,
 *   kecuali statusnya "DIBATALKAN" (ASUMSI nilai status ini, sesuaikan
 *   dengan daftar status resmi Order milik Moreno kalau beda).
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String STATUS_LUNAS = "LUNAS";
    private static final String TIPE_B2C = "B2C";
    private static final String TIPE_B2B = "B2B";
    private static final int TOP_PRODUK_LIMIT = 5;

    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;

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

    @Transactional(readOnly = true)
    public LaporanPenjualanResponse getLaporanPenjualan(LocalDate periodeMulai, LocalDate periodeAkhir) {
        LocalDateTime awal = periodeMulai.atStartOfDay();
        LocalDateTime akhir = periodeAkhir.atTime(23, 59, 59);

        List<OrderItem> items = orderItemRepository.findForLaporanPenjualan(awal, akhir);

        java.util.Set<UUID> idPesananUnik = new java.util.HashSet<>();
        java.util.Set<UUID> idPesananB2C = new java.util.HashSet<>();
        java.util.Set<UUID> idPesananB2B = new java.util.HashSet<>();

        int totalUnitTerjual = 0;
        int unitTerjualB2C = 0;
        int unitTerjualB2B = 0;

        // key = idProduk, value = [namaProduk, totalUnit, totalPenjualan]
        Map<UUID, ProdukAkumulasi> akumulasiPerProduk = new LinkedHashMap<>();

        for (OrderItem item : items) {
            UUID idOrder = item.getOrder().getIdOrder();
            idPesananUnik.add(idOrder);

            int qty = item.getQuantity() == null ? 0 : item.getQuantity();
            totalUnitTerjual += qty;

            String tipeOrder = item.getOrder().getTipeOrder();
            if (TIPE_B2C.equalsIgnoreCase(tipeOrder)) {
                idPesananB2C.add(idOrder);
                unitTerjualB2C += qty;
            } else if (TIPE_B2B.equalsIgnoreCase(tipeOrder)) {
                idPesananB2B.add(idOrder);
                unitTerjualB2B += qty;
            }

            UUID idProduk = item.getProductBatch().getProduct().getIdProduk();
            String namaProduk = item.getProductBatch().getProduct().getNamaProduk();
            BigDecimal subtotal = item.getSubtotalItem() == null ? BigDecimal.ZERO : item.getSubtotalItem();

            akumulasiPerProduk.computeIfAbsent(idProduk, k -> new ProdukAkumulasi(idProduk, namaProduk))
                    .tambah(qty, subtotal);
        }

        List<ProdukTerlarisResponse> produkTerlaris = akumulasiPerProduk.values().stream()
                .sorted(Comparator.comparingInt((ProdukAkumulasi p) -> p.totalUnit).reversed())
                .limit(TOP_PRODUK_LIMIT)
                .map(p -> new ProdukTerlarisResponse(p.idProduk, p.namaProduk, p.totalUnit, p.totalPenjualan))
                .toList();

        return new LaporanPenjualanResponse(
                periodeMulai,
                periodeAkhir,
                (long) idPesananUnik.size(),
                totalUnitTerjual,
                (long) idPesananB2C.size(),
                (long) idPesananB2B.size(),
                unitTerjualB2C,
                unitTerjualB2B,
                produkTerlaris
        );
    }

    /** Helper akumulasi internal — tidak dipakai di luar method getLaporanPenjualan(). */
    private static class ProdukAkumulasi {
        private final UUID idProduk;
        private final String namaProduk;
        private int totalUnit = 0;
        private BigDecimal totalPenjualan = BigDecimal.ZERO;

        ProdukAkumulasi(UUID idProduk, String namaProduk) {
            this.idProduk = idProduk;
            this.namaProduk = namaProduk;
        }

        void tambah(int qty, BigDecimal subtotal) {
            this.totalUnit += qty;
            this.totalPenjualan = this.totalPenjualan.add(subtotal);
        }
    }
}

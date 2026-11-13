package com.lezzat.b2b2c.frozenfood.service.core;

import com.lezzat.b2b2c.frozenfood.entity.ExpiryAlert;
import com.lezzat.b2b2c.frozenfood.entity.ProductBatch;
import com.lezzat.b2b2c.frozenfood.repository.ExpiryAlertRepository;
import com.lezzat.b2b2c.frozenfood.repository.ProductBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpiryAlertScheduler {

    private final ProductBatchRepository productBatchRepository;
    private final ExpiryAlertRepository expiryAlertRepository;

    private static final int THRESHOLD_DAYS = 30;
    private static final int CLEARANCE_DAYS = 7;
    private static final BigDecimal CLEARANCE_DISCOUNT = new BigDecimal("50.00");
    private static final BigDecimal NEAR_EXPIRY_DISCOUNT = new BigDecimal("20.00");

    // TESTING MODE: Jalan setiap pergantian menit (60 detik)
    @Scheduled(cron = "0 * * * * ?") 
    @Transactional
    public void scanAndGenerateExpiryAlerts() {
        log.info("///// [CRON-JOB] Memulai pemindaian stok kedaluwarsa...");

        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(THRESHOLD_DAYS);
        LocalDate clearanceDate = today.plusDays(CLEARANCE_DAYS);

        // MENGGUNAKAN QUERY ENTERPRISE ANTI N+1
        List<ProductBatch> criticalBatches = productBatchRepository.findBatchesExpiringBefore(thresholdDate);

        int alertCount = 0;
        for (ProductBatch batch : criticalBatches) {
            boolean isClearance = !batch.getTanggalExp().isAfter(clearanceDate);
            if (isClearance) {
                if (batch.getHargaOverride() == null
                        && (batch.getDiskon() == null || batch.getDiskon().compareTo(CLEARANCE_DISCOUNT) < 0)) {
                    batch.setDiskon(CLEARANCE_DISCOUNT);
                    productBatchRepository.save(batch);
                }
            } else {
                // Opsi 1: Batch H-30 s/d H-8 otomatis dapat diskon FEFO Deal 20% jika belum diset Admin
                if (batch.getHargaOverride() == null
                        && (batch.getDiskon() == null || batch.getDiskon().compareTo(BigDecimal.ZERO) <= 0)) {
                    batch.setDiskon(NEAR_EXPIRY_DISCOUNT);
                    productBatchRepository.save(batch);
                }
            }

            BigDecimal effectiveDisc = batch.getDiskon() != null
                    ? batch.getDiskon()
                    : (isClearance ? CLEARANCE_DISCOUNT : NEAR_EXPIRY_DISCOUNT);
            String prefix = isClearance
                    ? "CLEARANCE " + effectiveDisc.stripTrailingZeros().toPlainString() + "%: "
                    : "DARURAT (FEFO DEAL " + effectiveDisc.stripTrailingZeros().toPlainString() + "%): ";
            String expectedMessage = prefix + "Produk " + batch.getProduct().getNamaProduk()
                    + " (Batch: " + batch.getIdProdBatch() + ") kedaluwarsa pada " + batch.getTanggalExp();

            var existingAlertOpt = expiryAlertRepository
                    .findFirstByProductBatchAndIsResolvedFalseOrderByCreatedAtDesc(batch);

            if (existingAlertOpt.isEmpty()) {
                ExpiryAlert alert = ExpiryAlert.builder()
                        .productBatch(batch)
                        .pesan(expectedMessage)
                        .build();

                expiryAlertRepository.save(alert);
                alertCount++;
                log.warn("⚠️ [ALERT DIBUAT] {}", alert.getPesan());
            } else if (isClearance && !existingAlertOpt.get().getPesan().startsWith("CLEARANCE")) {
                ExpiryAlert existingAlert = existingAlertOpt.get();
                existingAlert.setPesan(expectedMessage);
                expiryAlertRepository.save(existingAlert);
                log.warn("⚠️ [ALERT DIPERBARUI KE CLEARANCE] {}", existingAlert.getPesan());
            }
        }

        log.info(" [CRON-JOB] Pemindaian selesai. {} peringatan baru dibuat.", alertCount);
    }
}
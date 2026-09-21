package com.lezzat.b2b2c.frozenfood.controller;

import com.lezzat.b2b2c.frozenfood.dto.response.ReportResponse;
import com.lezzat.b2b2c.frozenfood.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // Contoh panggilan: GET /api/reports/omzet?mulai=2026-09-01&akhir=2026-09-30
    @GetMapping("/omzet")
    public ResponseEntity<ReportResponse> getLaporanOmzet(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mulai,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate akhir) {
        return ResponseEntity.ok(reportService.getLaporanOmzet(mulai, akhir));
    }
}

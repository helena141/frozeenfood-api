package com.lezzat.b2b2c.frozenfood.controller;

import com.lezzat.b2b2c.frozenfood.service.NotaPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// UC-17 (Unduh Nota Pesanan Satuan) & UC-26 (Unduh Nota Pesanan Grosir).
// Satu endpoint generik untuk keduanya karena Order sudah membedakan lewat
// tipeOrder (B2C/B2B) — nota-nya cuma beda label "Satuan"/"Grosir" di isi PDF.
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class NotaController {

    private final NotaPdfService notaPdfService;

    @GetMapping(value = "/{idOrder}/nota", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadNota(@PathVariable UUID idOrder) {
        byte[] pdfBytes = notaPdfService.generateNotaPdf(idOrder);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("nota-" + idOrder + ".pdf")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
        headers.setContentType(MediaType.APPLICATION_PDF);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}

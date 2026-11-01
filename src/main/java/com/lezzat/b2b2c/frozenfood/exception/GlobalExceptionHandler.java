package com.lezzat.b2b2c.frozenfood.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// CATATAN: ini cross-cutting concern, bukan milik satu modul tertentu di
// pembagian tugas vertical-slicing. Dibuat di sini supaya endpoint modul
// Delivery/Payment/Report bisa dites dengan respons error yang rapi
// (400/404), bukan 500 generik. KOORDINASIKAN dengan Bony & Moreno sebelum
// merge — kalau salah satu dari mereka sudah bikin handler serupa,
// gabungkan jadi satu class supaya tidak bentrok/duplikat @RestControllerAdvice.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DeliveryRadiusExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRadiusExceeded(DeliveryRadiusExceededException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStock(InsufficientStockException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 1 order hanya boleh 1 payment (UNIQUE id_order).
    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicatePayment(DuplicatePaymentException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Pelanggaran constraint database (UNIQUE/FK/CHECK/NOT NULL). Pesan SQL
    // sengaja TIDAK dikirim ke klien supaya struktur database tidak bocor;
    // detail lengkap tetap tercatat di log server.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return buildResponse(HttpStatus.CONFLICT,
                "Data tidak dapat diproses karena melanggar aturan data (mis. data ganda atau relasi tidak valid)");
    }

    // JSON rusak atau tipe salah, mis. idUser bukan UUID.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Format request tidak valid (cek JSON dan tipe data, mis. UUID)");
    }

    // Parameter URL salah tipe, mis. /api/deliveries/abc padahal harus UUID.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Parameter '" + ex.getName() + "' bernilai tidak valid");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}

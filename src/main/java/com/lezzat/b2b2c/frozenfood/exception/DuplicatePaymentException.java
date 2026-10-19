package com.lezzat.b2b2c.frozenfood.exception;

/**
 * Dilempar saat pembayaran dibuat untuk order yang sudah punya pembayaran.
 * Tabel payments memakai UNIQUE(id_order), jadi 1 order hanya boleh 1 payment.
 * Dipetakan ke HTTP 409 oleh GlobalExceptionHandler.
 */
public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(String message) {
        super(message);
    }
}

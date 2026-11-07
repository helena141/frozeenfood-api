package com.lezzat.b2b2c.frozenfood.exception;

/** Perubahan status pengiriman tidak valid (lompat, mundur, atau status tak dikenal). HTTP 400. */
public class InvalidDeliveryStatusException extends RuntimeException {
    public InvalidDeliveryStatusException(String message) {
        super(message);
    }
}

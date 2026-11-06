package com.lezzat.b2b2c.frozenfood.exception;

/** Order sudah punya pengiriman (tabel deliveries UNIQUE id_order). HTTP 409. */
public class DuplicateDeliveryException extends RuntimeException {
    public DuplicateDeliveryException(String message) {
        super(message);
    }
}

package com.lezzat.b2b2c.frozenfood.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice // INI TAMENGNYA: Menangkap semua error dari seluruh Controller
public class GlobalExceptionHandler {

    // Menangkap error validasi bisnis (seperti "Email sudah terdaftar")
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage()); // Pesan error asli dari AuthService lu

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Menangkap error penyusup yang tidak lolos otorisasi/role
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", "Akses ditolak: Anda tidak memiliki hak (Role) untuk endpoint ini.");

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }
}
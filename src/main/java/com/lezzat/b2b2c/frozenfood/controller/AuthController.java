package com.lezzat.b2b2c.frozenfood.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lezzat.b2b2c.frozenfood.dto.request.LoginRequest;
import com.lezzat.b2b2c.frozenfood.dto.request.RegisterRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.AuthResponse;
import com.lezzat.b2b2c.frozenfood.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // Serahkan logika ke AuthService, kembalikan status 201 (CREATED)
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // Serahkan verifikasi ke AuthService, kembalikan status 200 (OK)
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
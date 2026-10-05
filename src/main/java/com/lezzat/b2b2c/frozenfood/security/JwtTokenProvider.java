package com.lezzat.b2b2c.frozenfood.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    // Rahasia ini disuntikkan dari application.yml, dengan nilai default fallback jika kosong
    @Value("${app.jwt-secret:LezzatFrozenFoodSecretKeyForEnterpriseArchitecture2026!}")
    private String jwtSecret;

    // Default masa berlaku token: 1 Hari (86400000 ms)
    @Value("${app.jwt-expiration-milliseconds:86400000}")
    private long jwtExpirationDate;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

   public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        
        // Ekstrak Role B2B/B2C saat login
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + jwtExpirationDate);

        return Jwts.builder()
                .subject(username)
                .claim("role", role) // INJEKSI MUTLAK: Role sekarang tertanam di dalam token!
                .issuedAt(new Date())
                .expiration(expireDate)
                .signWith(key())
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Di lingkungan production, log exception ditangkap di sini
            return false;
        }
    }
}
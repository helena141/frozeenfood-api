package com.lezzat.b2b2c.frozenfood.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Component
public class GoogleTokenVerifier {

    public record GoogleProfile(String sub, String email, String nama, String foto) {
    }

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${lezzat.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleProfile verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new BadCredentialsException("Token Google tidak valid atau kedaluwarsa");
            }
            GoogleIdToken.Payload payload = token.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new BadCredentialsException("Email Google belum terverifikasi");
            }
            return new GoogleProfile(
                    payload.getSubject(),
                    payload.getEmail(),
                    (String) payload.get("name"),
                    (String) payload.get("picture"));
        } catch (GeneralSecurityException | IOException e) {
            throw new BadCredentialsException("Gagal memverifikasi token Google", e);
        }
    }
}
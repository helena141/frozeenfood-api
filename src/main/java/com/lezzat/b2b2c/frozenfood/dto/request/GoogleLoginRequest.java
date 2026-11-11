package com.lezzat.b2b2c.frozenfood.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "Token Google wajib diisi") String idToken
) {
}
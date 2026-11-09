package com.lezzat.b2b2c.frozenfood.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CustomerResponse {
    private UUID idUser;
    private String nama;
    private String email;
    private String noTelp;
    private String alamat;
    private String tipeAkun;
    private String fotoProfil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
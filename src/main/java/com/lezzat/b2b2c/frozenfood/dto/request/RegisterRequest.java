package com.lezzat.b2b2c.frozenfood.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Nama tidak boleh kosong")
    private String nama;

    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email tidak valid")
    private String email;

    @NotBlank(message = "Password tidak boleh kosong")
    @Size(min = 6, message = "Password minimal 6 karakter")
    private String password;

    @NotBlank(message = "Nomor telepon tidak boleh kosong")
    private String noTelp;

    @NotBlank(message = "Alamat tidak boleh kosong")
    private String alamat;

    @NotBlank(message = "Tipe akun wajib diisi (B2C atau B2B)")
    private String tipeAkun;
}
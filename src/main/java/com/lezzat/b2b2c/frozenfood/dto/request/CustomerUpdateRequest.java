package com.lezzat.b2b2c.frozenfood.dto.request;

import lombok.Data;

@Data
public class CustomerUpdateRequest {
    private String nama;
    private String email;
    private String noTelp;
    private String alamat;
    private String passwordBaru;
}
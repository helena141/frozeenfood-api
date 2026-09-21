package com.lezzat.b2b2c.frozenfood.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    
    private String token;
    private String nama;
    private String email;
    private String tipeAkun;

}
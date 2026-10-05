package com.lezzat.b2b2c.frozenfood.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    private String name;
    private String description;
    private BigDecimal price; // Harga eceran (B2C), wajib BigDecimal
    private String category;
}
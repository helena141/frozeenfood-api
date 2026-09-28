package com.lezzat.b2b2c.frozenfood.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCatalogResponse {
    private UUID id; // Wajib UUID
    private String name;
    private String description;
    private BigDecimal price; // Harga eceran
    private String category;
    private Integer totalStock; // Akumulasi stok dari seluruh ProductBatch yang valid
}
package com.lezzat.b2b2c.frozenfood.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(

        @NotNull(message = "idProduk wajib diisi")
        UUID idProduk,

        @NotNull(message = "quantity wajib diisi")
        @Min(value = 1, message = "quantity minimal 1")
        Integer quantity
) {
}

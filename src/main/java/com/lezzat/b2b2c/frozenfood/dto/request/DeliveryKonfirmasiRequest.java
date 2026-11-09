package com.lezzat.b2b2c.frozenfood.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Admin/owner mengonfirmasi pengiriman pihak ketiga dan mengisi ongkir manual. */
public record DeliveryKonfirmasiRequest(
        @NotNull @DecimalMin("0") BigDecimal ongkir,
        @NotBlank String namaPengantar
) {
}

package com.lezzat.b2b2c.frozenfood.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record OrderB2CRequest(

        @NotNull(message = "idUser (customer) wajib diisi")
        UUID idUser,

        @NotEmpty(message = "Minimal 1 item di keranjang")
        @Valid
        List<OrderItemRequest> items,

        String catatan
) {
}

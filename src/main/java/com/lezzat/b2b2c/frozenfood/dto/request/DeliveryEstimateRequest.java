package com.lezzat.b2b2c.frozenfood.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Pratinjau ongkir/jadwal. Isi idOrder ATAU items. Tidak menyimpan apa pun. */
public record DeliveryEstimateRequest(
        @NotNull Double latitudeTujuan,
        @NotNull Double longitudeTujuan,
        UUID idOrder,
        @Valid List<Item> items
) {
    public record Item(@NotNull String idProduk, @Min(1) int quantity) {
    }
}

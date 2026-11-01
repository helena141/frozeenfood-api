package com.lezzat.b2b2c.frozenfood.dto.request;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {

    @Valid
    @NotEmpty(message = "Daftar pesanan tidak boleh kosong")
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {

        @NotNull(message = "ID Produk tidak boleh kosong")
        @JsonAlias({"productId", "id_produk"})
        private UUID idProduk;

        @Min(value = 1, message = "Kuantitas minimal 1")
        private int quantity;
    }
}

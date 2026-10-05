package com.lezzat.b2b2c.frozenfood.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {

    @NotEmpty(message = "Daftar pesanan tidak boleh kosong")
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        @NotNull(message = "ID Batch Produk tidak boleh kosong")
        private UUID batchId;

        @Min(value = 1, message = "Kuantitas minimal 1")
        private int quantity;
    }
}
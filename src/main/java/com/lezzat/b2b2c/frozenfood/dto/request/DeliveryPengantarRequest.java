package com.lezzat.b2b2c.frozenfood.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeliveryPengantarRequest(@NotBlank String namaPengantar) {
}

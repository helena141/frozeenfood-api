package com.lezzat.b2b2c.frozenfood.controller;

import com.lezzat.b2b2c.frozenfood.dto.request.OrderB2CRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.OrderResponse;
import com.lezzat.b2b2c.frozenfood.service.OrderB2CService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// UC-09: Buat Pesanan Satuan. Endpoint checkout khusus B2C — bukan lewat
// OrderController milik Moreno, karena alur harga/stok B2C (harga eceran +
// potong FEFO) beda total dari B2B (auto-tiering).
@RestController
@RequestMapping("/api/orders/retail")
@RequiredArgsConstructor
public class OrderB2CController {

    private final OrderB2CService orderB2CService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderB2CRequest request) {
        OrderResponse response = orderB2CService.createRetailOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

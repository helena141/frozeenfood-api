package com.lezzat.b2b2c.frozenfood.controller;

import com.lezzat.b2b2c.frozenfood.dto.request.ProductBatchRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.ProductBatchResponse;
import com.lezzat.b2b2c.frozenfood.service.ProductBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/batches")
@RequiredArgsConstructor
public class ProductBatchController {

    private final ProductBatchService productBatchService;

    @PostMapping
    public ResponseEntity<ProductBatchResponse> createBatch(@Valid @RequestBody ProductBatchRequest request) {
        ProductBatchResponse response = productBatchService.createBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
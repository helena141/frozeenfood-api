package com.lezzat.b2b2c.frozenfood.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lezzat.b2b2c.frozenfood.dto.request.OrderRequest;
import com.lezzat.b2b2c.frozenfood.dto.response.OrderResponse;
import com.lezzat.b2b2c.frozenfood.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders(
            @RequestParam(required = false) String tipeOrder,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getAllOrders(tipeOrder, status));
    }

    @GetMapping("/{idOrder}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID idOrder) {
        return ResponseEntity.ok(orderService.getOrderById(idOrder));
    }

    @PatchMapping("/{idOrder}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID idOrder,
            @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(idOrder, status));
    }
}
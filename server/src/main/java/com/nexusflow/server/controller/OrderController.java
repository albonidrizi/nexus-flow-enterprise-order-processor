package com.nexusflow.server.controller;

import com.nexusflow.server.dto.OrderDto;
import com.nexusflow.server.entity.Order;
import com.nexusflow.server.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestBody OrderDto.OrderRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(service.createOrder(request, username));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getMyOrders(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(service.getUserOrders(username));
    }
}

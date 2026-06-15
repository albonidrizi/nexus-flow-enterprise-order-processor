package com.nexusflow.server.web.controller;

import com.nexusflow.server.application.service.OrderService;
import com.nexusflow.server.config.RequestCorrelationFilter;
import com.nexusflow.server.domain.model.OrderStatus;
import com.nexusflow.server.web.dto.OrderDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    public ResponseEntity<OrderDto.OrderResponse> createOrder(
            @Valid @RequestBody OrderDto.OrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = RequestCorrelationFilter.HEADER_NAME, required = false) String correlationId,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createOrder(request, username, idempotencyKey, correlationId));
    }

    @GetMapping
    public ResponseEntity<Page<OrderDto.OrderResponse>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String customer,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(service.findOrders(authentication.getName(), status, customer, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto.OrderResponse> getOrder(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(service.getOrder(id, authentication.getName()));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<OrderDto.OrderResponse> processPayment(
            @PathVariable Long id,
            @Valid @RequestBody OrderDto.PaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        return ResponseEntity.ok(service.processPayment(id, request, authentication.getName(), idempotencyKey));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<OrderDto.OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderDto.StatusUpdateRequest request,
            @RequestHeader(value = RequestCorrelationFilter.HEADER_NAME, required = false) String correlationId,
            Authentication authentication) {
        return ResponseEntity.ok(service.updateStatus(id, request, authentication.getName(), correlationId));
    }
}

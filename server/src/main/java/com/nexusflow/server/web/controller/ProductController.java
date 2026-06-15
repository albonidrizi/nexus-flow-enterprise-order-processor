package com.nexusflow.server.web.controller;

import com.nexusflow.server.application.service.ProductService;
import com.nexusflow.server.web.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @GetMapping
    public ResponseEntity<List<ProductDto.ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(service.getAllProducts());
    }
}

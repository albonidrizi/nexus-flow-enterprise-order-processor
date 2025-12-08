package com.nexusflow.server.service;

import com.nexusflow.server.entity.Product;
import com.nexusflow.server.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    @Transactional
    public Product createProduct(Product product) {
        if (product == null)
            throw new IllegalArgumentException("Product cannot be null");
        return repository.save(product);
    }

    // Additional methods for inventory updates if needed (e.g., restock)
}

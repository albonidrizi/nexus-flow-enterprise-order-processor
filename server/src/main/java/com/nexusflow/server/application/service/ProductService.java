package com.nexusflow.server.application.service;

import com.nexusflow.server.domain.model.Product;
import com.nexusflow.server.exception.ResourceNotFoundException;
import com.nexusflow.server.infrastructure.persistence.ProductRepository;
import com.nexusflow.server.web.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    @Transactional(readOnly = true)
    public List<ProductDto.ProductResponse> getAllProducts() {
        log.debug("Fetching all products");
        return repository.findAll().stream()
                .map(ProductDto.ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        log.debug("Fetching product with ID: {}", id);
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    @Transactional
    public ProductDto.ProductResponse createProduct(ProductDto.ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName().trim())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .reservedQuantity(0)
                .build();
        log.info("Creating new product: {}", product.getName());
        return ProductDto.ProductResponse.from(repository.save(product));
    }

    @Transactional
    public ProductDto.ProductResponse updateProduct(Long id, ProductDto.ProductRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        log.info("Updating product with ID: {}", id);

        Product product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        product.setName(request.getName().trim());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());

        return ProductDto.ProductResponse.from(repository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        log.info("Deleting product with ID: {}", id);

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with ID: " + id);
        }

        repository.deleteById(id);
    }
}

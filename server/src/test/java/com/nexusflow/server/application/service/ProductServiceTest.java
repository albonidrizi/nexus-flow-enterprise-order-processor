package com.nexusflow.server.application.service;

import com.nexusflow.server.domain.model.Product;
import com.nexusflow.server.exception.ResourceNotFoundException;
import com.nexusflow.server.infrastructure.persistence.ProductRepository;
import com.nexusflow.server.web.dto.ProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(BigDecimal.valueOf(99.99))
                .quantity(50)
                .reservedQuantity(5)
                .version(1L)
                .build();
    }

    @Test
    void getAllProductsReturnsInventoryProjection() {
        List<Product> products = Arrays.asList(product,
                Product.builder().id(2L).name("Product 2").price(BigDecimal.TEN).quantity(10).reservedQuantity(0).build());

        when(productRepository.findAll()).thenReturn(products);

        List<ProductDto.ProductResponse> result = productService.getAllProducts();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAvailableQuantity()).isEqualTo(45);
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProductByIdReturnsProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.getProductById(1L);

        assertThat(result.getName()).isEqualTo("Test Product");
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void getProductByIdThrowsWhenMissing() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(999L));
    }

    @Test
    void createProductPersistsValidatedRequest() {
        ProductDto.ProductRequest request = new ProductDto.ProductRequest("New Product", BigDecimal.valueOf(25), 8);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(77L);
            saved.setVersion(0L);
            return saved;
        });

        ProductDto.ProductResponse result = productService.createProduct(request);

        assertThat(result.getId()).isEqualTo(77L);
        assertThat(result.getReservedQuantity()).isZero();
        assertThat(result.getAvailableQuantity()).isEqualTo(8);
    }

    @Test
    void updateProductChangesTrackedFields() {
        ProductDto.ProductRequest request = new ProductDto.ProductRequest("Updated Product", BigDecimal.valueOf(199.99), 100);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDto.ProductResponse result = productService.updateProduct(1L, request);

        assertThat(result.getName()).isEqualTo("Updated Product");
        assertThat(result.getQuantity()).isEqualTo(100);
    }

    @Test
    void updateProductThrowsWhenMissing() {
        ProductDto.ProductRequest request = new ProductDto.ProductRequest("Updated", BigDecimal.TEN, 5);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.updateProduct(999L, request));
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProductDeletesExistingProduct() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProductThrowsWhenMissing() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> productService.deleteProduct(999L));
        verify(productRepository, never()).deleteById(any());
    }
}

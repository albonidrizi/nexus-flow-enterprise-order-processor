package com.nexusflow.server.web.dto;

import com.nexusflow.server.domain.model.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class ProductDto {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductRequest {
        @NotBlank(message = "Product name is required")
        @Size(max = 160, message = "Product name must be at most 160 characters")
        private String name;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        private BigDecimal price;

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        private Integer quantity;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductResponse {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer quantity;
        private Integer reservedQuantity;
        private Integer availableQuantity;
        private Long version;

        public static ProductResponse from(Product product) {
            return ProductResponse.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .price(product.getPrice())
                    .quantity(product.getQuantity())
                    .reservedQuantity(product.getReservedQuantity())
                    .availableQuantity(product.availableQuantity())
                    .version(product.getVersion())
                    .build();
        }
    }
}

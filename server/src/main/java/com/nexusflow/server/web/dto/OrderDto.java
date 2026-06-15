package com.nexusflow.server.web.dto;

import com.nexusflow.server.domain.model.Order;
import com.nexusflow.server.domain.model.OrderEvent;
import com.nexusflow.server.domain.model.OrderItem;
import com.nexusflow.server.domain.model.OrderStatus;
import com.nexusflow.server.domain.model.PaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderRequest {
        @NotEmpty(message = "Order must contain at least one item")
        @Size(max = 25, message = "Order cannot contain more than 25 line items")
        @Valid
        private List<OrderItemRequest> items;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemRequest {
        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentRequest {
        @Builder.Default
        private boolean approved = true;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatusUpdateRequest {
        @NotNull(message = "Target status is required")
        private OrderStatus status;

        @Size(max = 500, message = "Note must be at most 500 characters")
        private String note;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderResponse {
        private Long id;
        private String customer;
        private BigDecimal totalAmount;
        private OrderStatus status;
        private PaymentStatus paymentStatus;
        private String idempotencyKey;
        private String correlationId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long version;
        private List<OrderItemResponse> items;
        private List<OrderEventResponse> history;

        public static OrderResponse from(Order order) {
            return OrderResponse.builder()
                    .id(order.getId())
                    .customer(order.getUser().getUsername())
                    .totalAmount(order.getTotalAmount())
                    .status(order.getStatus())
                    .paymentStatus(order.getPaymentStatus())
                    .idempotencyKey(order.getIdempotencyKey())
                    .correlationId(order.getCorrelationId())
                    .createdAt(order.getCreatedAt())
                    .updatedAt(order.getUpdatedAt())
                    .version(order.getVersion())
                    .items(order.getItems().stream().map(OrderItemResponse::from).toList())
                    .history(order.getEvents().stream().map(OrderEventResponse::from).toList())
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemResponse {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;

        public static OrderItemResponse from(OrderItem item) {
            return OrderItemResponse.builder()
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getPrice())
                    .lineTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderEventResponse {
        private String eventType;
        private OrderStatus fromStatus;
        private OrderStatus toStatus;
        private String actor;
        private String correlationId;
        private String note;
        private LocalDateTime createdAt;

        public static OrderEventResponse from(OrderEvent event) {
            return OrderEventResponse.builder()
                    .eventType(event.getEventType())
                    .fromStatus(event.getFromStatus())
                    .toStatus(event.getToStatus())
                    .actor(event.getActor())
                    .correlationId(event.getCorrelationId())
                    .note(event.getNote())
                    .createdAt(event.getCreatedAt())
                    .build();
        }
    }
}

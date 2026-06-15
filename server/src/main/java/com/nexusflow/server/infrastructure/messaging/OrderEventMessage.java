package com.nexusflow.server.infrastructure.messaging;

import com.nexusflow.server.domain.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventMessage {
    private Long orderId;
    private String eventType;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private String actor;
    private String correlationId;
    private LocalDateTime occurredAt;
}

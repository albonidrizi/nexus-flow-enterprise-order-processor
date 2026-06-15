package com.nexusflow.server.config;

import com.nexusflow.server.domain.model.OrderStatus;
import com.nexusflow.server.infrastructure.persistence.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("orderProcessing")
@RequiredArgsConstructor
public class OrderProcessingHealthIndicator implements HealthIndicator {

    private final OrderRepository orderRepository;

    @Override
    public Health health() {
        long failedOrders = orderRepository.countByStatus(OrderStatus.FAILED);
        long processingOrders = orderRepository.countByStatus(OrderStatus.PROCESSING);

        return Health.up()
                .withDetail("failedOrders", failedOrders)
                .withDetail("processingOrders", processingOrders)
                .build();
    }
}

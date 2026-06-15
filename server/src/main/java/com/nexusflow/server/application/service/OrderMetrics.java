package com.nexusflow.server.application.service;

import com.nexusflow.server.domain.model.OrderStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {

    private final MeterRegistry meterRegistry;

    public OrderMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordCreated() {
        Counter.builder("nexusflow.orders.created")
                .description("Number of orders created")
                .register(meterRegistry)
                .increment();
    }

    public void recordTransition(OrderStatus toStatus) {
        Counter.builder("nexusflow.orders.status.transitions")
                .tag("to", toStatus.name())
                .description("Order status transitions by target status")
                .register(meterRegistry)
                .increment();
    }
}

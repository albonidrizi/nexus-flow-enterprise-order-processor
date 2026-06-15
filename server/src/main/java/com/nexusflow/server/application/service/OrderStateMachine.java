package com.nexusflow.server.application.service;

import com.nexusflow.server.domain.model.OrderStatus;
import com.nexusflow.server.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class OrderStateMachine {

    private final Map<OrderStatus, Set<OrderStatus>> transitions = new EnumMap<>(OrderStatus.class);

    public OrderStateMachine() {
        transitions.put(OrderStatus.CREATED, EnumSet.of(OrderStatus.VALIDATED, OrderStatus.CANCELLED, OrderStatus.FAILED));
        transitions.put(OrderStatus.VALIDATED, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED, OrderStatus.FAILED));
        transitions.put(OrderStatus.PAID, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED, OrderStatus.FAILED));
        transitions.put(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.FAILED));
        transitions.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
        transitions.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        transitions.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        transitions.put(OrderStatus.FAILED, EnumSet.noneOf(OrderStatus.class));
    }

    public void assertTransition(OrderStatus from, OrderStatus to) {
        if (from == to) {
            return;
        }
        if (!transitions.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessRuleException("Invalid order status transition from " + from + " to " + to);
        }
    }
}

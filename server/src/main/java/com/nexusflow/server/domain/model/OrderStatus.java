package com.nexusflow.server.domain.model;

public enum OrderStatus {
    CREATED,
    VALIDATED,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    FAILED;

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == FAILED;
    }
}

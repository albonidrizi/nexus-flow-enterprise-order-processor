package com.nexusflow.server.infrastructure.messaging;

public enum OutboxStatus {
    PENDING,
    FAILED,
    PUBLISHED,
    DEAD
}

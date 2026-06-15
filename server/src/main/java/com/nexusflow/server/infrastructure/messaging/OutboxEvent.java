package com.nexusflow.server.infrastructure.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String messageId;

    @Column(nullable = false, length = 80)
    private String aggregateType;

    @Column(nullable = false, length = 80)
    private String aggregateId;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, length = 120)
    private String routingKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OutboxStatus status;

    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(length = 120)
    private String correlationId;

    @Column(columnDefinition = "text")
    private String lastError;

    @Column(nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    public static OutboxEvent pending(
            String aggregateType,
            String aggregateId,
            String eventType,
            String routingKey,
            String correlationId,
            String payload) {
        LocalDateTime now = LocalDateTime.now();
        return OutboxEvent.builder()
                .messageId(UUID.randomUUID().toString())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .routingKey(routingKey)
                .correlationId(correlationId)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .build();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (messageId == null) {
            messageId = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = OutboxStatus.PENDING;
        }
        if (attempts == null) {
            attempts = 0;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = now;
        }
    }

    public void markPublished(LocalDateTime publishedAt) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(String error, LocalDateTime nextAttemptAt, int maxAttempts) {
        this.attempts = attempts == null ? 1 : attempts + 1;
        this.lastError = truncate(error, 2_000);
        this.status = this.attempts >= maxAttempts ? OutboxStatus.DEAD : OutboxStatus.FAILED;
        this.nextAttemptAt = nextAttemptAt;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}

package com.nexusflow.server.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.server.domain.model.OrderStatus;
import com.nexusflow.server.infrastructure.persistence.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxService {

    private static final String AGGREGATE_TYPE_ORDER = "Order";
    private static final String EVENT_TYPE_STATUS_CHANGED = "ORDER_STATUS_CHANGED";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void enqueueStatusChanged(
            Long orderId,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String actor,
            String correlationId) {
        OrderEventMessage message = OrderEventMessage.builder()
                .orderId(orderId)
                .eventType(EVENT_TYPE_STATUS_CHANGED)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .actor(actor)
                .correlationId(correlationId)
                .occurredAt(LocalDateTime.now())
                .build();

        OutboxEvent event = OutboxEvent.pending(
                AGGREGATE_TYPE_ORDER,
                String.valueOf(orderId),
                EVENT_TYPE_STATUS_CHANGED,
                RabbitMQConfig.ORDER_STATUS_CHANGED_ROUTING_KEY,
                correlationId,
                serialize(message));

        outboxEventRepository.save(event);
        log.info("Enqueued order outbox event orderId={} from={} to={} correlationId={}",
                orderId, fromStatus, toStatus, correlationId);
    }

    private String serialize(OrderEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize order event message", e);
        }
    }
}

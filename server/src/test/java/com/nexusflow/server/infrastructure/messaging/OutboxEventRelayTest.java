package com.nexusflow.server.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexusflow.server.domain.model.OrderStatus;
import com.nexusflow.server.infrastructure.persistence.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventRelayTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    private ObjectMapper objectMapper;
    private OutboxEventRelay outboxEventRelay;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        outboxEventRelay = new OutboxEventRelay(outboxEventRepository, orderEventPublisher, objectMapper);
        ReflectionTestUtils.setField(outboxEventRelay, "batchSize", 10);
        ReflectionTestUtils.setField(outboxEventRelay, "maxAttempts", 3);
    }

    @Test
    void publishPendingEventsPublishesAndMarksEventPublished() throws Exception {
        OutboxEvent event = outboxEvent();
        when(outboxEventRepository.findPublishable(any(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(event));

        outboxEventRelay.publishPendingEvents();

        verify(orderEventPublisher).publish(any(OrderEventMessage.class), eq(event.getRoutingKey()), eq(event.getMessageId()));
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void publishPendingEventsMarksEventDeadAfterFinalAttempt() throws Exception {
        OutboxEvent event = outboxEvent();
        event.setAttempts(2);
        when(outboxEventRepository.findPublishable(any(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(event));
        doThrow(new IllegalStateException("RabbitMQ unavailable"))
                .when(orderEventPublisher).publish(any(OrderEventMessage.class), eq(event.getRoutingKey()), eq(event.getMessageId()));

        outboxEventRelay.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(event.getAttempts()).isEqualTo(3);
        assertThat(event.getLastError()).contains("RabbitMQ unavailable");
        assertThat(event.getNextAttemptAt()).isAfter(LocalDateTime.now());
    }

    private OutboxEvent outboxEvent() throws Exception {
        OrderEventMessage message = OrderEventMessage.builder()
                .orderId(42L)
                .eventType("ORDER_STATUS_CHANGED")
                .fromStatus(OrderStatus.VALIDATED)
                .toStatus(OrderStatus.PAID)
                .actor("testuser")
                .correlationId("corr-42")
                .occurredAt(LocalDateTime.now())
                .build();

        return OutboxEvent.pending(
                "Order",
                "42",
                "ORDER_STATUS_CHANGED",
                RabbitMQConfig.ORDER_STATUS_CHANGED_ROUTING_KEY,
                "corr-42",
                objectMapper.writeValueAsString(message));
    }
}

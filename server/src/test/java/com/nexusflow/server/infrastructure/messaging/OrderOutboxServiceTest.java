package com.nexusflow.server.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexusflow.server.domain.model.OrderStatus;
import com.nexusflow.server.infrastructure.persistence.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderOutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OrderOutboxService orderOutboxService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        orderOutboxService = new OrderOutboxService(outboxEventRepository, objectMapper);
    }

    @Test
    void enqueueStatusChangedPersistsDurableOutboxEvent() throws Exception {
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderOutboxService.enqueueStatusChanged(
                42L,
                OrderStatus.VALIDATED,
                OrderStatus.PAID,
                "testuser",
                "corr-42");

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());

        OutboxEvent event = eventCaptor.getValue();
        assertThat(event.getMessageId()).hasSize(36);
        assertThat(event.getAggregateType()).isEqualTo("Order");
        assertThat(event.getAggregateId()).isEqualTo("42");
        assertThat(event.getEventType()).isEqualTo("ORDER_STATUS_CHANGED");
        assertThat(event.getRoutingKey()).isEqualTo(RabbitMQConfig.ORDER_STATUS_CHANGED_ROUTING_KEY);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getCorrelationId()).isEqualTo("corr-42");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        OrderEventMessage payload = objectMapper.readValue(event.getPayload(), OrderEventMessage.class);
        assertThat(payload.getOrderId()).isEqualTo(42L);
        assertThat(payload.getFromStatus()).isEqualTo(OrderStatus.VALIDATED);
        assertThat(payload.getToStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(payload.getActor()).isEqualTo("testuser");
        assertThat(payload.getCorrelationId()).isEqualTo("corr-42");
    }
}

package com.nexusflow.server.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(OrderEventMessage message, String routingKey, String messageId) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                routingKey,
                message,
                rabbitMessage -> {
                    rabbitMessage.getMessageProperties().setCorrelationId(message.getCorrelationId());
                    rabbitMessage.getMessageProperties().setMessageId(messageId);
                    return rabbitMessage;
                });

        log.info("Published order event orderId={} eventType={} routingKey={} messageId={} correlationId={}",
                message.getOrderId(), message.getEventType(), routingKey, messageId, message.getCorrelationId());
    }
}

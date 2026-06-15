package com.nexusflow.server.infrastructure.messaging;

import com.nexusflow.server.application.service.OrderService;
import com.nexusflow.server.domain.model.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderStatusChangedListener {

    private final OrderService orderService;

    public OrderStatusChangedListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_EVENTS_QUEUE)
    public void handleOrderStatusChanged(OrderEventMessage message) {
        log.info("Received order event orderId={} toStatus={} correlationId={}",
                message.getOrderId(), message.getToStatus(), message.getCorrelationId());

        if (message.getToStatus() == OrderStatus.PAID) {
            orderService.advancePaidOrderWorkflow(message.getOrderId(), message.getCorrelationId());
        }
    }
}

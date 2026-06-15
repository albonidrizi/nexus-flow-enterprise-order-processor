package com.nexusflow.server.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "nexusflow.orders";
    public static final String ORDER_DEAD_LETTER_EXCHANGE = "nexusflow.orders.dlx";
    public static final String ORDER_EVENTS_QUEUE = "nexusflow.order-events";
    public static final String ORDER_EVENTS_DLQ = "nexusflow.order-events.dlq";
    public static final String ORDER_STATUS_CHANGED_ROUTING_KEY = "order.status.changed";
    public static final String ORDER_DEAD_LETTER_ROUTING_KEY = "order.status.dead";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange orderDeadLetterExchange() {
        return new TopicExchange(ORDER_DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderEventsQueue() {
        return QueueBuilder.durable(ORDER_EVENTS_QUEUE)
                .deadLetterExchange(ORDER_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(ORDER_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderEventsDeadLetterQueue() {
        return QueueBuilder.durable(ORDER_EVENTS_DLQ).build();
    }

    @Bean
    public Binding orderEventsBinding() {
        return BindingBuilder.bind(orderEventsQueue())
                .to(orderExchange())
                .with(ORDER_STATUS_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding orderEventsDeadLetterBinding() {
        return BindingBuilder.bind(orderEventsDeadLetterQueue())
                .to(orderDeadLetterExchange())
                .with(ORDER_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

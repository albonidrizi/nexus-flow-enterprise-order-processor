package com.nexusflow.server.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.server.infrastructure.persistence.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxEventRelay {

    private static final List<OutboxStatus> PUBLISHABLE_STATUSES = List.of(
            OutboxStatus.PENDING,
            OutboxStatus.FAILED);

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.batch-size:25}")
    private int batchSize;

    @Value("${app.outbox.max-attempts:8}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findPublishable(
                PUBLISHABLE_STATUSES,
                LocalDateTime.now(),
                PageRequest.of(0, batchSize));

        events.forEach(this::publishOne);
    }

    private void publishOne(OutboxEvent event) {
        try {
            OrderEventMessage message = objectMapper.readValue(event.getPayload(), OrderEventMessage.class);
            orderEventPublisher.publish(message, event.getRoutingKey(), event.getMessageId());
            event.markPublished(LocalDateTime.now());
            log.info("Published outbox event id={} messageId={} eventType={}",
                    event.getId(), event.getMessageId(), event.getEventType());
        } catch (Exception e) {
            event.markFailed(e.getMessage(), nextAttemptAt(event), maxAttempts);
            log.warn("Outbox event publish failed id={} messageId={} attempts={} status={}",
                    event.getId(), event.getMessageId(), event.getAttempts(), event.getStatus());
            log.debug("Outbox publish failure details", e);
        }
    }

    private LocalDateTime nextAttemptAt(OutboxEvent event) {
        int nextAttempt = event.getAttempts() == null ? 1 : event.getAttempts() + 1;
        long delaySeconds = Math.min(300, (long) Math.pow(2, nextAttempt));
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }
}

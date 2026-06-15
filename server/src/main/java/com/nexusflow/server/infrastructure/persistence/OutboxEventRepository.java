package com.nexusflow.server.infrastructure.persistence;

import com.nexusflow.server.infrastructure.messaging.OutboxEvent;
import com.nexusflow.server.infrastructure.messaging.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select event from OutboxEvent event
            where event.status in :statuses
              and event.nextAttemptAt <= :now
            order by event.createdAt asc
            """)
    List<OutboxEvent> findPublishable(
            @Param("statuses") Collection<OutboxStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable);
}

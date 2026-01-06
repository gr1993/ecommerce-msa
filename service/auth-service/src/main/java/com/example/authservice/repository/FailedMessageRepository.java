package com.example.authservice.repository;

import com.example.authservice.domain.entity.FailedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FailedMessageRepository extends JpaRepository<FailedMessage, Long> {

    List<FailedMessage> findByStatus(FailedMessage.FailedMessageStatus status);

    List<FailedMessage> findByTopic(String topic);

    List<FailedMessage> findByEventType(String eventType);

    List<FailedMessage> findByFailedAtAfter(LocalDateTime dateTime);

    List<FailedMessage> findByStatusOrderByFailedAtAsc(FailedMessage.FailedMessageStatus status);

    List<FailedMessage> findByStatusAndRetryCountLessThan(
        FailedMessage.FailedMessageStatus status,
        Integer maxRetryCount
    );

    void deleteByStatusAndProcessedAtBefore(
        FailedMessage.FailedMessageStatus status,
        LocalDateTime dateTime
    );
}

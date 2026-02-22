package com.example.shippingservice.returns.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnEventPublisher {

    private static final String TOPIC_RETURN_COMPLETED = "return.completed";
    private static final int SEND_TIMEOUT_SECONDS = 5;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishReturnCompleted(ReturnCompletedEvent event) {
        try {
            kafkaTemplate.send(TOPIC_RETURN_COMPLETED, String.valueOf(event.getOrderId()), event)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("return.completed 이벤트 발행 완료 - returnId={}, orderId={}",
                    event.getReturnId(), event.getOrderId());
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("return.completed 이벤트 발행 실패 - returnId={}, orderId={}",
                    event.getReturnId(), event.getOrderId(), e);
            throw new RuntimeException("return.completed 이벤트 발행 실패", e);
        }
    }
}

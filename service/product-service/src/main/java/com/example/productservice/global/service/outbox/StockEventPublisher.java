package com.example.productservice.global.service.outbox;

import com.example.productservice.global.common.EventTypeConstants;
import com.example.productservice.global.domain.Outbox;
import com.example.productservice.product.domain.event.StockRejectedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding.KafkaAsyncKey;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding.KafkaAsyncMessageBinding;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation.Headers;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.mapping.AbstractJavaTypeMapper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @AsyncPublisher(
            operation = @AsyncOperation(
                    channelName = EventTypeConstants.TOPIC_STOCK_REJECTED,
                    description = "재고 부족 이벤트 발행 - 보상 트랜잭션 트리거",
                    payloadType = StockRejectedEvent.class,
                    headers = @Headers(
                            schemaName = "StockRejectedEventHeaders",
                            values = {
                                    @Headers.Header(
                                            name = AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
                                            description = "Spring Kafka 타입 ID 헤더 - Consumer의 TYPE_MAPPINGS와 매핑됨",
                                            value = EventTypeConstants.TYPE_ID_STOCK_REJECTED
                                    )
                            }
                    )
            )
    )
    @KafkaAsyncOperationBinding(
            messageBinding = @KafkaAsyncMessageBinding(
                    key = @KafkaAsyncKey(
                            description = "집계 타입과 집계 ID를 조합한 키 (형식: {aggregateType}-{aggregateId})",
                            example = "Order-123"
                    )
            )
    )
    public void publishStockRejectedEvent(Outbox outbox) throws JsonProcessingException {
        StockRejectedEvent event = objectMapper.readValue(outbox.getPayload(), StockRejectedEvent.class);

        String key = outbox.getAggregateType() + "-" + outbox.getAggregateId();

        kafkaTemplate.send(
                EventTypeConstants.TOPIC_STOCK_REJECTED,
                key,
                event
        );

        log.info("StockRejectedEvent 발행 완료: orderId={}, orderNumber={}, reason={}",
                event.getOrderId(), event.getOrderNumber(), event.getRejectionReason());
    }
}

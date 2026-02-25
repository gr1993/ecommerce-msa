package com.example.shippingservice.exchange.scheduler;

import com.example.shippingservice.client.dto.TrackingDetail;
import com.example.shippingservice.client.dto.TrackingInfoResponse;
import com.example.shippingservice.domain.entity.Outbox;
import com.example.shippingservice.domain.event.ExchangeCollectingEvent;
import com.example.shippingservice.domain.event.ExchangeReturnCompletedEvent;
import com.example.shippingservice.exchange.entity.OrderExchange;
import com.example.shippingservice.exchange.enums.ExchangeStatus;
import com.example.shippingservice.exchange.repository.OrderExchangeHistoryRepository;
import com.example.shippingservice.exchange.repository.OrderExchangeRepository;
import com.example.shippingservice.global.common.EventTypeConstants;
import com.example.shippingservice.repository.OutboxRepository;
import com.example.shippingservice.shipping.enums.CarrierCode;
import com.example.shippingservice.shipping.service.MockDeliveryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeCollectionScheduler {

    private final OrderExchangeRepository orderExchangeRepository;
    private final OrderExchangeHistoryRepository orderExchangeHistoryRepository;
    private final MockDeliveryService mockDeliveryService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRateString = "${shipping.tracking.poll-interval-ms:60000}")
    @Transactional
    public void pollCollectionStatuses() {
        List<OrderExchange> targets = orderExchangeRepository.findByExchangeStatusIn(
                List.of(ExchangeStatus.EXCHANGE_APPROVED, ExchangeStatus.EXCHANGE_COLLECTING)
        );

        if (targets.isEmpty()) {
            log.debug("교환 회수 추적 폴링: 추적 대상 없음");
            return;
        }

        log.info("교환 회수 추적 폴링 시작 - 대상: {}건", targets.size());

        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (OrderExchange orderExchange : targets) {
            try {
                boolean changed = pollAndUpdate(orderExchange);
                if (changed) updated++;
                else skipped++;
            } catch (Exception e) {
                log.error("교환 회수 상태 업데이트 실패 - exchangeId: {}, trackingNumber: {}",
                        orderExchange.getExchangeId(), orderExchange.getTrackingNumber(), e);
                failed++;
            }
        }

        log.info("교환 회수 추적 폴링 완료 - 업데이트: {}건, 변경없음: {}건, 실패: {}건", updated, skipped, failed);
    }

    private boolean pollAndUpdate(OrderExchange orderExchange) {
        String courierCode = resolveCarrierCode(orderExchange.getCourier());

        TrackingInfoResponse response = (courierCode != null)
                ? mockDeliveryService.getTrackingInfo(courierCode, orderExchange.getTrackingNumber())
                : mockDeliveryService.getTrackingInfo(orderExchange.getTrackingNumber());

        if (response == null || response.getLastDetail() == null) {
            log.warn("배송 조회 응답 없음 - exchangeId: {}, trackingNumber: {}",
                    orderExchange.getExchangeId(), orderExchange.getTrackingNumber());
            return false;
        }

        TrackingDetail lastDetail = response.getLastDetail();
        String currentKind = lastDetail.getKind();

        if (currentKind == null) {
            log.debug("배송 상태 kind 없음 - exchangeId: {}", orderExchange.getExchangeId());
            return false;
        }

        // 마지막으로 기록된 외부 택배사 상태(kind) 조회
        Optional<String> lastTrackingKind = orderExchangeHistoryRepository
                .findLastTrackingKindByExchangeId(orderExchange.getExchangeId());

        // 외부 택배사 상태가 동일하면 업데이트 불필요
        if (lastTrackingKind.isPresent() && lastTrackingKind.get().equals(currentKind)) {
            log.debug("외부 택배사 상태 변경 없음 - exchangeId: {}, kind: {}",
                    orderExchange.getExchangeId(), currentKind);
            return false;
        }

        ExchangeStatus previousStatus = orderExchange.getExchangeStatus();
        ExchangeStatus mappedStatus = mapToInternalStatus(currentKind);

        if (mappedStatus != null) {
            // 상태 업데이트 및 히스토리 추가
            orderExchange.updateExchangeStatus(mappedStatus);
            orderExchange.addExchangeHistory(
                    previousStatus,
                    mappedStatus,
                    lastDetail.getWhere(),
                    lastDetail.getRemark(),
                    currentKind,
                    "DELIVERY_API"
            );

            log.info("교환 회수 상태 업데이트 - exchangeId: {}, orderId: {}, "
                            + "exchangeStatus: {} → {}, kind: {}",
                    orderExchange.getExchangeId(), orderExchange.getOrderId(),
                    previousStatus, mappedStatus, currentKind);

            // 이벤트 발행
            if (previousStatus != ExchangeStatus.EXCHANGE_COLLECTING
                    && mappedStatus == ExchangeStatus.EXCHANGE_COLLECTING) {
                saveExchangeCollectingOutbox(orderExchange);
            } else if (previousStatus != ExchangeStatus.EXCHANGE_RETURN_COMPLETED
                    && mappedStatus == ExchangeStatus.EXCHANGE_RETURN_COMPLETED) {
                saveExchangeReturnCompletedOutbox(orderExchange);
            }
        } else {
            // 내부 상태 변경 없이 이력만 추가
            orderExchange.addExchangeHistory(
                    previousStatus,
                    previousStatus,
                    lastDetail.getWhere(),
                    lastDetail.getRemark(),
                    currentKind,
                    "DELIVERY_API"
            );

            log.info("교환 회수 이력 추가 (상태 변경 없음) - exchangeId: {}, orderId: {}, kind: {}",
                    orderExchange.getExchangeId(), orderExchange.getOrderId(), currentKind);
        }

        return true;
    }

    private ExchangeStatus mapToInternalStatus(String kind) {
        if (kind == null) return null;
        return switch (kind) {
            case "IN_TRANSIT", "AT_DESTINATION", "OUT_FOR_DELIVERY" -> ExchangeStatus.EXCHANGE_COLLECTING;
            case "DELIVERED" -> ExchangeStatus.EXCHANGE_RETURN_COMPLETED;
            default -> null; // ACCEPTED, PICKED_UP
        };
    }

    private String resolveCarrierCode(String shippingCompany) {
        if (shippingCompany == null || shippingCompany.isBlank()) {
            return null;
        }
        try {
            return CarrierCode.fromName(shippingCompany).getCode();
        } catch (IllegalArgumentException e) {
            log.warn("택배사 코드 조회 실패, 기본값 사용 - shippingCompany: {}", shippingCompany);
            return null;
        }
    }

    private void saveExchangeCollectingOutbox(OrderExchange orderExchange) {
        ExchangeCollectingEvent event = ExchangeCollectingEvent.builder()
                .exchangeId(orderExchange.getExchangeId())
                .orderId(orderExchange.getOrderId())
                .userId(orderExchange.getUserId())
                .courier(orderExchange.getCourier())
                .trackingNumber(orderExchange.getTrackingNumber())
                .collectingAt(LocalDateTime.now())
                .build();

        saveOutbox(orderExchange.getExchangeId(), EventTypeConstants.TOPIC_EXCHANGE_COLLECTING, event);
    }

    private void saveExchangeReturnCompletedOutbox(OrderExchange orderExchange) {
        ExchangeReturnCompletedEvent event = ExchangeReturnCompletedEvent.builder()
                .exchangeId(orderExchange.getExchangeId())
                .orderId(orderExchange.getOrderId())
                .userId(orderExchange.getUserId())
                .courier(orderExchange.getCourier())
                .trackingNumber(orderExchange.getTrackingNumber())
                .returnCompletedAt(LocalDateTime.now())
                .build();

        saveOutbox(orderExchange.getExchangeId(), EventTypeConstants.TOPIC_EXCHANGE_RETURN_COMPLETED, event);
    }

    private void saveOutbox(Long exchangeId, String eventType, Object eventPayload) {
        try {
            String payload = objectMapper.writeValueAsString(eventPayload);
            Outbox outbox = Outbox.builder()
                    .aggregateType("Exchange")
                    .aggregateId(String.valueOf(exchangeId))
                    .eventType(eventType)
                    .payload(payload)
                    .build();
            outboxRepository.save(outbox);
            log.debug("Outbox 이벤트 저장 완료: exchangeId={}, eventType={}", exchangeId, eventType);
        } catch (JsonProcessingException e) {
            log.error("Outbox 이벤트 직렬화 실패: exchangeId={}, eventType={}", exchangeId, eventType, e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}

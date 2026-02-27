package com.example.shippingservice.exchange.scheduler;

import com.example.shippingservice.client.dto.TrackingDetail;
import com.example.shippingservice.client.dto.TrackingInfoResponse;
import com.example.shippingservice.domain.entity.Outbox;
import com.example.shippingservice.domain.event.ExchangeCompletedEvent;
import com.example.shippingservice.exchange.dto.ExchangeItemDto;
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
import java.util.stream.Collectors;

/**
 * 교환 배송 상태 추적 스케줄러
 *
 * EXCHANGE_SHIPPING 상태의 교환 건을 대상으로 외부 택배사 API를 주기적으로 폴링하여
 * 배송 완료(DELIVERED) 감지 시 EXCHANGED 로 자동 전이합니다.
 *
 * 외부 API kind 값 → 내부 상태 매핑:
 * - ACCEPTED, PICKED_UP, IN_TRANSIT, AT_DESTINATION, OUT_FOR_DELIVERY → 이력만 추가 (상태 변경 없음)
 * - DELIVERED → EXCHANGED + exchange.completed 이벤트 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeShippingScheduler {

    private final OrderExchangeRepository orderExchangeRepository;
    private final OrderExchangeHistoryRepository orderExchangeHistoryRepository;
    private final MockDeliveryService mockDeliveryService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRateString = "${shipping.tracking.poll-interval-ms:60000}")
    @Transactional
    public void pollShippingStatuses() {
        List<OrderExchange> targets = orderExchangeRepository.findByExchangeStatusIn(
                List.of(ExchangeStatus.EXCHANGE_SHIPPING)
        );

        if (targets.isEmpty()) {
            log.debug("교환 배송 추적 폴링: 추적 대상 없음");
            return;
        }

        log.info("교환 배송 추적 폴링 시작 - 대상: {}건", targets.size());

        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (OrderExchange orderExchange : targets) {
            try {
                boolean changed = pollAndUpdate(orderExchange);
                if (changed) updated++;
                else skipped++;
            } catch (Exception e) {
                log.error("교환 배송 상태 업데이트 실패 - exchangeId: {}, trackingNumber: {}",
                        orderExchange.getExchangeId(), orderExchange.getTrackingNumber(), e);
                failed++;
            }
        }

        log.info("교환 배송 추적 폴링 완료 - 업데이트: {}건, 변경없음: {}건, 실패: {}건", updated, skipped, failed);
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

        // 배송 구간 전용 마지막 kind 조회 (회수 구간 이력과 구분)
        Optional<String> lastTrackingKind = orderExchangeHistoryRepository
                .findLastTrackingKindByExchangeIdAndTrackingNumber(
                        orderExchange.getExchangeId(), orderExchange.getTrackingNumber());

        if (lastTrackingKind.isPresent() && lastTrackingKind.get().equals(currentKind)) {
            log.debug("외부 택배사 상태 변경 없음 - exchangeId: {}, kind: {}",
                    orderExchange.getExchangeId(), currentKind);
            return false;
        }

        ExchangeStatus previousStatus = orderExchange.getExchangeStatus();

        if ("DELIVERED".equals(currentKind)) {
            orderExchange.updateExchangeStatus(ExchangeStatus.EXCHANGED);
            orderExchange.addExchangeHistory(
                    previousStatus,
                    ExchangeStatus.EXCHANGED,
                    lastDetail.getWhere(),
                    lastDetail.getRemark(),
                    currentKind,
                    "DELIVERY_API"
            );

            log.info("교환 배송 완료 - exchangeId: {}, orderId: {}", orderExchange.getExchangeId(), orderExchange.getOrderId());
            saveExchangeCompletedOutbox(orderExchange);
        } else {
            // IN_TRANSIT, AT_DESTINATION, OUT_FOR_DELIVERY, ACCEPTED, PICKED_UP 등 → 이력만 추가
            orderExchange.addExchangeHistory(
                    previousStatus,
                    previousStatus,
                    lastDetail.getWhere(),
                    lastDetail.getRemark(),
                    currentKind,
                    "DELIVERY_API"
            );

            log.info("교환 배송 이력 추가 (상태 변경 없음) - exchangeId: {}, orderId: {}, kind: {}",
                    orderExchange.getExchangeId(), orderExchange.getOrderId(), currentKind);
        }

        return true;
    }

    private String resolveCarrierCode(String courier) {
        if (courier == null || courier.isBlank()) {
            return null;
        }
        try {
            return CarrierCode.fromName(courier).getCode();
        } catch (IllegalArgumentException e) {
            log.warn("택배사 코드 조회 실패, 기본값 사용 - courier: {}", courier);
            return null;
        }
    }

    private void saveExchangeCompletedOutbox(OrderExchange orderExchange) {
        List<ExchangeItemDto> exchangeItems = orderExchange.getExchangeItems().stream()
                .map(item -> ExchangeItemDto.builder()
                        .orderItemId(item.getOrderItemId())
                        .originalOptionId(item.getOriginalOptionId())
                        .newOptionId(item.getNewOptionId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        ExchangeCompletedEvent event = ExchangeCompletedEvent.builder()
                .exchangeId(orderExchange.getExchangeId())
                .orderId(orderExchange.getOrderId())
                .userId(orderExchange.getUserId())
                .exchangeItems(exchangeItems)
                .completedAt(LocalDateTime.now())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);
            Outbox outbox = Outbox.builder()
                    .aggregateType("Exchange")
                    .aggregateId(String.valueOf(orderExchange.getExchangeId()))
                    .eventType(EventTypeConstants.TOPIC_EXCHANGE_COMPLETED)
                    .payload(payload)
                    .build();
            outboxRepository.save(outbox);
            log.debug("교환 완료 Outbox 저장 완료: exchangeId={}", orderExchange.getExchangeId());
        } catch (JsonProcessingException e) {
            log.error("교환 완료 이벤트 직렬화 실패: exchangeId={}", orderExchange.getExchangeId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}

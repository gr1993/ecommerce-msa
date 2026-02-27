package com.example.shippingservice.exchange.service;

import com.example.shippingservice.client.dto.BulkUploadItem;
import com.example.shippingservice.client.dto.BulkUploadResponse;
import com.example.shippingservice.client.dto.BulkUploadResult;
import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.domain.entity.Outbox;
import com.example.shippingservice.domain.event.ExchangeApprovedEvent;
import com.example.shippingservice.exchange.dto.ExchangeItemDto;
import com.example.shippingservice.exchange.dto.request.AdminExchangeApproveRequest;
import com.example.shippingservice.exchange.dto.request.AdminExchangeRejectRequest;
import com.example.shippingservice.exchange.dto.request.AdminExchangeShippingRequest;
import com.example.shippingservice.exchange.dto.response.AdminExchangeResponse;
import com.example.shippingservice.exchange.entity.OrderExchange;
import com.example.shippingservice.exchange.enums.ExchangeStatus;
import com.example.shippingservice.exchange.repository.OrderExchangeRepository;
import com.example.shippingservice.global.common.EventTypeConstants;
import com.example.shippingservice.repository.OutboxRepository;
import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.CarrierCode;
import com.example.shippingservice.shipping.repository.OrderShippingRepository;
import com.example.shippingservice.shipping.service.MockDeliveryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminExchangeServiceImpl implements AdminExchangeService {

    private final OrderExchangeRepository orderExchangeRepository;
    private final OrderShippingRepository orderShippingRepository;
    private final MockDeliveryService mockDeliveryService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public PageResponse<AdminExchangeResponse> getExchanges(String exchangeStatus, Long orderId, Pageable pageable) {
        ExchangeStatus status = parseExchangeStatus(exchangeStatus);
        Page<OrderExchange> exchangePage = orderExchangeRepository.findAllBySearchCondition(status, orderId, pageable);
        Page<AdminExchangeResponse> responsePage = exchangePage.map(AdminExchangeResponse::from);
        return PageResponse.from(responsePage);
    }

    @Override
    public AdminExchangeResponse getExchange(Long exchangeId) {
        return AdminExchangeResponse.from(findExchangeById(exchangeId));
    }

    @Override
    @Transactional
    public AdminExchangeResponse approveExchange(Long exchangeId, AdminExchangeApproveRequest request) {
        OrderExchange orderExchange = findExchangeById(exchangeId);

        if (orderExchange.getExchangeStatus() != ExchangeStatus.EXCHANGE_REQUESTED) {
            throw new IllegalStateException(
                    "교환 신청 상태에서만 승인할 수 있습니다. 현재 상태: " + orderExchange.getExchangeStatus());
        }

        // 회수 수거지 정보 저장
        orderExchange.updateCollectInfo(
                request.getCollectReceiverName(),
                request.getCollectReceiverPhone(),
                request.getCollectAddress(),
                request.getCollectPostalCode()
        );

        ExchangeStatus previousStatus = orderExchange.getExchangeStatus();
        orderExchange.updateExchangeStatus(ExchangeStatus.EXCHANGE_APPROVED);

        // Mock 택배사 API로 회수 운송장 자동 발급
        String collectTrackingNumber = issueTrackingNumber(
                request.getCollectReceiverName(),
                request.getCollectReceiverPhone(),
                request.getCollectAddress(),
                "회수상품"
        );
        if (collectTrackingNumber != null) {
            orderExchange.updateCollectTrackingInfo("CJ대한통운", collectTrackingNumber);
            log.info("회수 운송장 발급 완료 - exchangeId={}, trackingNumber={}", exchangeId, collectTrackingNumber);
            orderExchange.addExchangeHistory(
                    previousStatus,
                    ExchangeStatus.EXCHANGE_APPROVED,
                    "회수 접수",
                    "회수 운송장 정상 발급",
                    "ACCEPTED",
                    "ADMIN"
            );
        } else {
            log.warn("회수 운송장 발급 실패 - exchangeId={}", exchangeId);
        }

        log.info("교환 승인 완료 - exchangeId={}, orderId={}", exchangeId, orderExchange.getOrderId());

        // Outbox에 교환 승인 이벤트 저장
        saveExchangeApprovedOutbox(orderExchange);

        return AdminExchangeResponse.from(orderExchange);
    }

    @Override
    @Transactional
    public AdminExchangeResponse rejectExchange(Long exchangeId, AdminExchangeRejectRequest request) {
        OrderExchange orderExchange = findExchangeById(exchangeId);

        if (orderExchange.getExchangeStatus() != ExchangeStatus.EXCHANGE_REQUESTED) {
            throw new IllegalStateException(
                    "교환 신청 상태에서만 거절할 수 있습니다. 현재 상태: " + orderExchange.getExchangeStatus());
        }

        orderExchange.reject(request.getRejectReason());
        log.info("교환 거절 완료 - exchangeId={}, orderId={}", exchangeId, orderExchange.getOrderId());
        return AdminExchangeResponse.from(orderExchange);
    }

    @Override
    @Transactional
    public AdminExchangeResponse startShipping(Long exchangeId, AdminExchangeShippingRequest request) {
        OrderExchange orderExchange = findExchangeById(exchangeId);

        if (orderExchange.getExchangeStatus() != ExchangeStatus.EXCHANGE_RETURN_COMPLETED) {
            throw new IllegalStateException(
                    "회수 완료 상태에서만 교환 배송을 시작할 수 있습니다. 현재 상태: " + orderExchange.getExchangeStatus());
        }

        // 원주문 배송지 조회
        OrderShipping orderShipping = orderShippingRepository.findByOrderId(orderExchange.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                        "원주문 배송 정보를 찾을 수 없습니다. orderId=" + orderExchange.getOrderId()));

        // 원주문 배송지를 교환품 배송지로 저장
        orderExchange.updateExchangeAddress(
                orderShipping.getReceiverName(),
                orderShipping.getReceiverPhone(),
                orderShipping.getAddress(),
                orderShipping.getPostalCode()
        );

        CarrierCode carrier = CarrierCode.fromCode(request.getCarrierCode());
        ExchangeStatus previousStatus = orderExchange.getExchangeStatus();
        orderExchange.updateExchangeStatus(ExchangeStatus.EXCHANGE_SHIPPING);

        // Mock 택배사 API로 교환품 배송 운송장 자동 발급
        String trackingNumber = issueTrackingNumber(
                orderShipping.getReceiverName(),
                orderShipping.getReceiverPhone(),
                orderShipping.getAddress(),
                "교환상품"
        );
        if (trackingNumber != null) {
            orderExchange.updateTrackingInfo(carrier.getName(), trackingNumber);
            log.info("교환품 배송 운송장 발급 완료 - exchangeId={}, trackingNumber={}", exchangeId, trackingNumber);
            orderExchange.addExchangeHistory(
                    previousStatus,
                    ExchangeStatus.EXCHANGE_SHIPPING,
                    "배송 준비 중",
                    "교환품 배송 운송장 정상 발급",
                    "ACCEPTED",
                    "ADMIN"
            );
        } else {
            log.warn("교환품 배송 운송장 발급 실패 - exchangeId={}", exchangeId);
        }

        log.info("교환 배송 시작 - exchangeId={}, orderId={}", exchangeId, orderExchange.getOrderId());
        return AdminExchangeResponse.from(orderExchange);
    }

    private void saveExchangeApprovedOutbox(OrderExchange orderExchange) {
        List<ExchangeItemDto> itemDtos = orderExchange.getExchangeItems().stream()
                .map(item -> ExchangeItemDto.builder()
                        .orderItemId(item.getOrderItemId())
                        .originalOptionId(item.getOriginalOptionId())
                        .newOptionId(item.getNewOptionId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        ExchangeApprovedEvent event = ExchangeApprovedEvent.builder()
                .exchangeId(orderExchange.getExchangeId())
                .orderId(orderExchange.getOrderId())
                .userId(orderExchange.getUserId())
                .exchangeItems(itemDtos)
                .collectCourier(orderExchange.getCollectCourier())
                .collectTrackingNumber(orderExchange.getCollectTrackingNumber())
                .approvedAt(LocalDateTime.now())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);
            Outbox outbox = Outbox.builder()
                    .aggregateType("Exchange")
                    .aggregateId(String.valueOf(orderExchange.getExchangeId()))
                    .eventType(EventTypeConstants.TOPIC_EXCHANGE_APPROVED)
                    .payload(payload)
                    .build();
            outboxRepository.save(outbox);
            log.info("교환 승인 Outbox 저장 완료 - exchangeId={}", orderExchange.getExchangeId());
        } catch (JsonProcessingException e) {
            log.error("교환 승인 이벤트 직렬화 실패 - exchangeId={}", orderExchange.getExchangeId(), e);
            throw new RuntimeException("교환 승인 이벤트 직렬화 실패", e);
        }
    }

    private String issueTrackingNumber(String receiverName, String receiverPhone,
                                       String address, String goodsName) {
        BulkUploadItem item = BulkUploadItem.builder()
                .receiverName(receiverName)
                .receiverPhone1(receiverPhone)
                .receiverAddress(address)
                .goodsName(goodsName)
                .goodsQty(1)
                .build();

        BulkUploadResponse response = mockDeliveryService.bulkUpload(List.of(item));

        if (response.getIsSuccess() && !response.getResults().isEmpty()) {
            BulkUploadResult result = response.getResults().get(0);
            if (result.getIsSuccess()) {
                return result.getTrackingNumber();
            }
        }
        return null;
    }

    private OrderExchange findExchangeById(Long exchangeId) {
        return orderExchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new IllegalArgumentException("교환 정보를 찾을 수 없습니다. exchangeId=" + exchangeId));
    }

    private ExchangeStatus parseExchangeStatus(String exchangeStatus) {
        if (exchangeStatus == null || exchangeStatus.isBlank()) {
            return null;
        }
        try {
            return ExchangeStatus.valueOf(exchangeStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 교환 상태입니다: " + exchangeStatus);
        }
    }
}

package com.example.shippingservice.returns.service;

import com.example.shippingservice.client.dto.BulkUploadItem;
import com.example.shippingservice.client.dto.BulkUploadResponse;
import com.example.shippingservice.client.dto.BulkUploadResult;
import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.domain.entity.Outbox;
import com.example.shippingservice.global.common.EventTypeConstants;
import com.example.shippingservice.repository.OutboxRepository;
import com.example.shippingservice.returns.dto.request.AdminReturnApproveRequest;
import com.example.shippingservice.returns.dto.request.AdminReturnRejectRequest;
import com.example.shippingservice.returns.dto.response.AdminReturnResponse;
import com.example.shippingservice.returns.entity.OrderReturn;
import com.example.shippingservice.returns.enums.ReturnStatus;
import com.example.shippingservice.domain.event.ReturnApprovedEvent;
import com.example.shippingservice.domain.event.ReturnCompletedEvent;
import com.example.shippingservice.returns.repository.OrderReturnRepository;
import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.ShippingStatus;
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
public class AdminReturnServiceImpl implements AdminReturnService {

    private final OrderReturnRepository orderReturnRepository;
    private final OrderShippingRepository orderShippingRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final MockDeliveryService mockDeliveryService;

    @Override
    public PageResponse<AdminReturnResponse> getReturns(String returnStatus, String orderNumber, Pageable pageable) {
        ReturnStatus status = parseReturnStatus(returnStatus);

        Page<OrderReturn> returnPage = orderReturnRepository.findAllBySearchCondition(status, orderNumber, pageable);
        Page<AdminReturnResponse> responsePage = returnPage.map(AdminReturnResponse::from);
        return PageResponse.from(responsePage);
    }

    @Override
    public AdminReturnResponse getReturn(Long returnId) {
        OrderReturn orderReturn = findReturnById(returnId);
        return AdminReturnResponse.from(orderReturn);
    }

    @Override
    @Transactional
    public AdminReturnResponse approveReturn(Long returnId, AdminReturnApproveRequest request) {
        OrderReturn orderReturn = findReturnById(returnId);

        if (orderReturn.getReturnStatus() != ReturnStatus.RETURN_REQUESTED) {
            throw new IllegalStateException(
                    "반품 신청 상태에서만 승인할 수 있습니다. 현재 상태: " + orderReturn.getReturnStatus());
        }

        // 회수지(창고) 주소 설정
        orderReturn.updateReturnAddress(
                request.getReceiverName(),
                request.getReceiverPhone(),
                request.getReturnAddress(),
                request.getPostalCode()
        );
        orderReturn.updateReturnStatus(ReturnStatus.RETURN_APPROVED);

        // Mock 택배사 API로 회수 운송장 자동 발급
        String trackingNumber = issueReturnPickupTrackingNumber(orderReturn);
        if (trackingNumber != null) {
            orderReturn.updateTrackingInfo("CJ대한통운", trackingNumber);
            log.info("반품 회수 운송장 발급 완료 - returnId={}, trackingNumber={}", returnId, trackingNumber);
        } else {
            log.warn("반품 회수 운송장 발급 실패 - returnId={}, 수동 처리가 필요합니다.", returnId);
        }

        // order_shipping에 이력 추가 (상태는 DELIVERED 유지)
        OrderShipping shipping = orderShippingRepository.findByOrderId(orderReturn.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "배송 정보를 찾을 수 없습니다. orderId=" + orderReturn.getOrderId()));

        String remarkMessage = trackingNumber != null
                ? "반품 승인됨 - 회수 운송장: " + trackingNumber
                : "반품 승인됨 - 운송장 미발급";

        shipping.addTrackingDetail(
                "반품 수거지",
                remarkMessage,
                "RETURN_APPROVED"
        );

        log.info("반품 승인 완료 - returnId={}, orderId={}, shippingId={}",
                returnId, orderReturn.getOrderId(), shipping.getShippingId());

        // return.approved 이벤트 Outbox 저장
        saveReturnApprovedOutbox(orderReturn, trackingNumber);

        return AdminReturnResponse.from(orderReturn);
    }

    /**
     * 반품 회수 운송장 발급
     * Mock 택배사 API를 통해 회수 지시를 내리고 운송장 번호를 발급받습니다.
     * 택배 기사가 사용자 주소로 방문하여 물품을 회수해 창고로 배송합니다.
     */
    private String issueReturnPickupTrackingNumber(OrderReturn orderReturn) {
        BulkUploadItem item = BulkUploadItem.builder()
                .receiverName(orderReturn.getReceiverName())
                .receiverPhone1(orderReturn.getReceiverPhone())
                .receiverAddress(orderReturn.getReturnAddress())
                .goodsName("반품회수")
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

    @Override
    @Transactional
    public AdminReturnResponse rejectReturn(Long returnId, AdminReturnRejectRequest request) {
        OrderReturn orderReturn = findReturnById(returnId);

        if (orderReturn.getReturnStatus() != ReturnStatus.RETURN_REQUESTED) {
            throw new IllegalStateException(
                    "반품 신청 상태에서만 거절할 수 있습니다. 현재 상태: " + orderReturn.getReturnStatus());
        }

        orderReturn.reject(request.getRejectReason());

        log.info("반품 거절 완료 - returnId={}, orderId={}, reason={}",
                returnId, orderReturn.getOrderId(), request.getRejectReason());

        return AdminReturnResponse.from(orderReturn);
    }

    @Override
    @Transactional
    public AdminReturnResponse completeReturn(Long returnId) {
        OrderReturn orderReturn = findReturnById(returnId);

        if (orderReturn.getReturnStatus() != ReturnStatus.RETURN_APPROVED) {
            throw new IllegalStateException(
                    "승인된 반품 건만 완료 처리할 수 있습니다. 현재 상태: " + orderReturn.getReturnStatus());
        }

        // 반품 완료 처리
        orderReturn.updateReturnStatus(ReturnStatus.RETURNED);

        // order_shipping 상태도 RETURNED로 변경
        OrderShipping shipping = orderShippingRepository.findByOrderId(orderReturn.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "배송 정보를 찾을 수 없습니다. orderId=" + orderReturn.getOrderId()));

        shipping.updateShippingStatus(ShippingStatus.RETURNED, "RETURN_COMPLETED");

        log.info("반품 완료 처리 - returnId={}, orderId={}, shippingId={}",
                returnId, orderReturn.getOrderId(), shipping.getShippingId());

        // return.completed 이벤트 Outbox 저장 (환불 + 재고 복구 트리거)
        saveReturnCompletedOutbox(orderReturn);

        return AdminReturnResponse.from(orderReturn);
    }

    private void saveReturnApprovedOutbox(OrderReturn orderReturn, String trackingNumber) {
        ReturnApprovedEvent event = ReturnApprovedEvent.builder()
                .returnId(orderReturn.getReturnId())
                .orderId(orderReturn.getOrderId())
                .userId(orderReturn.getUserId())
                .courier(orderReturn.getCourier())
                .trackingNumber(trackingNumber)
                .approvedAt(LocalDateTime.now())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);
            Outbox outbox = Outbox.builder()
                    .aggregateType("Return")
                    .aggregateId(String.valueOf(orderReturn.getReturnId()))
                    .eventType(EventTypeConstants.TOPIC_RETURN_APPROVED)
                    .payload(payload)
                    .build();
            outboxRepository.save(outbox);
            log.debug("ReturnApprovedEvent Outbox 저장 완료: returnId={}", orderReturn.getReturnId());
        } catch (JsonProcessingException e) {
            log.error("ReturnApprovedEvent 직렬화 실패: returnId={}", orderReturn.getReturnId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }

    private void saveReturnCompletedOutbox(OrderReturn orderReturn) {
        ReturnCompletedEvent event = ReturnCompletedEvent.builder()
                .returnId(orderReturn.getReturnId())
                .orderId(orderReturn.getOrderId())
                .userId(orderReturn.getUserId())
                .reason(orderReturn.getReason())
                .completedAt(LocalDateTime.now())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);
            Outbox outbox = Outbox.builder()
                    .aggregateType("Return")
                    .aggregateId(String.valueOf(orderReturn.getReturnId()))
                    .eventType(EventTypeConstants.TOPIC_RETURN_COMPLETED)
                    .payload(payload)
                    .build();
            outboxRepository.save(outbox);
            log.debug("ReturnCompletedEvent Outbox 저장 완료: returnId={}", orderReturn.getReturnId());
        } catch (JsonProcessingException e) {
            log.error("ReturnCompletedEvent 직렬화 실패: returnId={}", orderReturn.getReturnId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }

    private OrderReturn findReturnById(Long returnId) {
        return orderReturnRepository.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("반품 정보를 찾을 수 없습니다. returnId=" + returnId));
    }

    private ReturnStatus parseReturnStatus(String returnStatus) {
        if (returnStatus == null || returnStatus.isBlank()) {
            return null;
        }
        try {
            return ReturnStatus.valueOf(returnStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 반품 상태입니다: " + returnStatus);
        }
    }
}

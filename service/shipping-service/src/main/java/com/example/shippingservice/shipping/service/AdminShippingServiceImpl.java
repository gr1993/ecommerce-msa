package com.example.shippingservice.shipping.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.shipping.dto.request.RegisterTrackingRequest;
import com.example.shippingservice.shipping.dto.response.AdminShippingResponse;
import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.CarrierCode;
import com.example.shippingservice.shipping.enums.DeliveryServiceStatus;
import com.example.shippingservice.shipping.enums.ShippingStatus;
import com.example.shippingservice.shipping.repository.OrderShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminShippingServiceImpl implements AdminShippingService {

    private final OrderShippingRepository orderShippingRepository;
    private final MockDeliveryService mockDeliveryService;

    @Override
    public PageResponse<AdminShippingResponse> getShippings(String shippingStatus, String trackingNumber, Pageable pageable) {
        ShippingStatus status = parseShippingStatus(shippingStatus);

        Page<OrderShipping> shippingPage = orderShippingRepository.findAllBySearchCondition(
                status,
                trackingNumber != null && trackingNumber.isBlank() ? null : trackingNumber,
                pageable
        );

        Page<AdminShippingResponse> responsePage = shippingPage.map(AdminShippingResponse::from);
        return PageResponse.from(responsePage);
    }

    /**
     * 운송장 등록
     * 외부 택배사 API를 호출하여 운송장을 발급하고 배송 정보를 업데이트합니다.
     *
     * @param shippingId 배송 ID
     * @param request    운송장 등록 요청 (택배사 코드)
     * @return 업데이트된 배송 정보
     */
    @Override
    @Transactional
    public AdminShippingResponse registerTracking(Long shippingId, RegisterTrackingRequest request) {
        OrderShipping orderShipping = orderShippingRepository.findById(shippingId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보를 찾을 수 없습니다. shippingId=" + shippingId));

        if (orderShipping.getDeliveryServiceStatus() != DeliveryServiceStatus.NOT_SENT) {
            throw new IllegalStateException(
                    "이미 택배사에 전송된 배송 건입니다. 현재 상태: " + orderShipping.getDeliveryServiceStatus()
            );
        }

        CarrierCode carrier = CarrierCode.fromCode(request.getCarrierCode());

        log.info("운송장 발급 요청 - shippingId: {}, carrierCode: {}, orderNumber: {}",
                shippingId, carrier.getCode(), orderShipping.getOrderNumber());

        String trackingNumber = mockDeliveryService.issueSingleTrackingNumber(orderShipping);

        if (trackingNumber == null) {
            throw new RuntimeException("택배사 API 호출에 실패하여 운송장을 발급하지 못했습니다. shippingId=" + shippingId);
        }

        orderShipping.updateTrackingInfo(carrier.getName(), trackingNumber);
        orderShipping.updateDeliveryServiceStatus(DeliveryServiceStatus.SENT);

        log.info("운송장 발급 완료 - shippingId: {}, carrier: {}, trackingNumber: {}",
                shippingId, carrier.getName(), trackingNumber);

        return AdminShippingResponse.from(orderShipping);
    }

    private ShippingStatus parseShippingStatus(String shippingStatus) {
        if (shippingStatus == null || shippingStatus.isBlank()) {
            return null;
        }
        try {
            return ShippingStatus.valueOf(shippingStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 배송 상태입니다: " + shippingStatus);
        }
    }
}

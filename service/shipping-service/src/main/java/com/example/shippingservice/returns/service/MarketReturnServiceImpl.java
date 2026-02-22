package com.example.shippingservice.returns.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.returns.dto.request.ReturnTrackingRequest;
import com.example.shippingservice.returns.dto.response.MarketReturnResponse;
import com.example.shippingservice.returns.entity.OrderReturn;
import com.example.shippingservice.returns.enums.ReturnStatus;
import com.example.shippingservice.returns.repository.OrderReturnRepository;
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
public class MarketReturnServiceImpl implements MarketReturnService {

    private final OrderReturnRepository orderReturnRepository;

    @Override
    public PageResponse<MarketReturnResponse> getMyReturns(Long userId, Pageable pageable) {
        Page<OrderReturn> returnPage = orderReturnRepository.findByUserId(userId, pageable);
        Page<MarketReturnResponse> responsePage = returnPage.map(MarketReturnResponse::from);
        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional
    public MarketReturnResponse registerTracking(Long userId, Long returnId, ReturnTrackingRequest request) {
        OrderReturn orderReturn = orderReturnRepository.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("반품 정보를 찾을 수 없습니다. returnId=" + returnId));

        if (!orderReturn.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 반품 건만 수정할 수 있습니다.");
        }

        if (orderReturn.getReturnStatus() != ReturnStatus.RETURN_APPROVED) {
            throw new IllegalStateException(
                    "승인된 반품 건에만 운송장을 등록할 수 있습니다. 현재 상태: " + orderReturn.getReturnStatus());
        }

        orderReturn.updateTrackingInfo(request.getCourier(), request.getTrackingNumber());

        log.info("반품 운송장 등록 완료 - returnId={}, courier={}, trackingNumber={}",
                returnId, request.getCourier(), request.getTrackingNumber());

        return MarketReturnResponse.from(orderReturn);
    }
}

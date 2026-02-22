package com.example.shippingservice.returns.service;

import com.example.shippingservice.exchange.enums.ExchangeStatus;
import com.example.shippingservice.exchange.repository.OrderExchangeRepository;
import com.example.shippingservice.returns.dto.request.InternalCreateReturnRequest;
import com.example.shippingservice.returns.dto.response.InternalCreateReturnResponse;
import com.example.shippingservice.returns.entity.OrderReturn;
import com.example.shippingservice.returns.enums.ReturnStatus;
import com.example.shippingservice.returns.repository.OrderReturnRepository;
import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.ShippingStatus;
import com.example.shippingservice.shipping.repository.OrderShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalReturnServiceImpl implements InternalReturnService {

    private static final List<ReturnStatus> ACTIVE_RETURN_STATUSES = List.of(
            ReturnStatus.RETURN_REQUESTED, ReturnStatus.RETURN_APPROVED
    );

    private static final List<ExchangeStatus> ACTIVE_EXCHANGE_STATUSES = List.of(
            ExchangeStatus.EXCHANGE_REQUESTED, ExchangeStatus.EXCHANGE_APPROVED
    );

    private final OrderReturnRepository orderReturnRepository;
    private final OrderExchangeRepository orderExchangeRepository;
    private final OrderShippingRepository orderShippingRepository;

    @Override
    @Transactional
    public InternalCreateReturnResponse createReturn(InternalCreateReturnRequest request) {
        Long orderId = request.getOrderId();

        // 배송 상태 확인 (DELIVERED만 가능)
        OrderShipping shipping = orderShippingRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보를 찾을 수 없습니다. orderId=" + orderId));

        if (shipping.getShippingStatus() != ShippingStatus.DELIVERED) {
            throw new IllegalStateException(
                    "배송 완료 상태에서만 반품 신청이 가능합니다. 현재 상태: " + shipping.getShippingStatus());
        }

        // 진행 중인 반품 건 확인
        if (orderReturnRepository.existsByOrderIdAndReturnStatusIn(orderId, ACTIVE_RETURN_STATUSES)) {
            throw new IllegalStateException("이미 진행 중인 반품 건이 있습니다. orderId=" + orderId);
        }

        // 진행 중인 교환 건 확인
        if (orderExchangeRepository.existsByOrderIdAndExchangeStatusIn(orderId, ACTIVE_EXCHANGE_STATUSES)) {
            throw new IllegalStateException("진행 중인 교환 건이 있어 반품 신청이 불가합니다. orderId=" + orderId);
        }

        OrderReturn orderReturn = OrderReturn.builder()
                .orderId(orderId)
                .userId(request.getUserId())
                .returnStatus(ReturnStatus.RETURN_REQUESTED)
                .reason(request.getReason())
                .build();

        orderReturnRepository.save(orderReturn);

        log.info("반품 생성 완료 - returnId={}, orderId={}, userId={}",
                orderReturn.getReturnId(), orderId, request.getUserId());

        return InternalCreateReturnResponse.from(orderReturn);
    }
}

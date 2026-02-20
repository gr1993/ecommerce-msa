package com.example.shippingservice.shipping.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.shipping.dto.response.AdminShippingResponse;
import com.example.shippingservice.shipping.entity.OrderShipping;
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

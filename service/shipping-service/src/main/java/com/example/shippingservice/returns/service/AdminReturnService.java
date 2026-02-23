package com.example.shippingservice.returns.service;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.returns.dto.request.AdminReturnApproveRequest;
import com.example.shippingservice.returns.dto.request.AdminReturnRejectRequest;
import com.example.shippingservice.returns.dto.response.AdminReturnResponse;
import org.springframework.data.domain.Pageable;

public interface AdminReturnService {

    /**
     * 반품 목록을 조회합니다. (페이징, 필터)
     */
    PageResponse<AdminReturnResponse> getReturns(String returnStatus, String orderNumber, Pageable pageable);

    /**
     * 반품 상세를 조회합니다.
     */
    AdminReturnResponse getReturn(Long returnId);

    /**
     * 반품을 승인하고 수거지 정보를 설정합니다.
     */
    AdminReturnResponse approveReturn(Long returnId, AdminReturnApproveRequest request);

    /**
     * 반품을 거절합니다.
     */
    AdminReturnResponse rejectReturn(Long returnId, AdminReturnRejectRequest request);

    /**
     * 반품을 완료 처리합니다. order_shipping 상태도 RETURNED로 변경합니다.
     */
    AdminReturnResponse completeReturn(Long returnId);
}

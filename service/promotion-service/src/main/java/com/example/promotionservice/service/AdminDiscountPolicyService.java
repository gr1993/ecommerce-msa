package com.example.promotionservice.service;

import com.example.promotionservice.dto.request.AdminDiscountPolicyCreateRequest;
import com.example.promotionservice.dto.request.AdminDiscountPolicyUpdateRequest;
import com.example.promotionservice.dto.response.AdminDiscountPolicyDetailResponse;
import com.example.promotionservice.dto.response.AdminDiscountPolicyResponse;
import com.example.promotionservice.global.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdminDiscountPolicyService {

    PageResponse<AdminDiscountPolicyResponse> getDiscountPolicies(String keyword, String status, Pageable pageable);

    AdminDiscountPolicyDetailResponse getDiscountPolicyDetail(Long discountId);

    AdminDiscountPolicyDetailResponse createDiscountPolicy(AdminDiscountPolicyCreateRequest request);

    AdminDiscountPolicyDetailResponse updateDiscountPolicy(Long discountId, AdminDiscountPolicyUpdateRequest request);
}

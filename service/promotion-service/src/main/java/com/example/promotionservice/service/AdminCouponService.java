package com.example.promotionservice.service;

import com.example.promotionservice.dto.request.AdminCouponCreateRequest;
import com.example.promotionservice.dto.request.AdminCouponUpdateRequest;
import com.example.promotionservice.dto.response.AdminCouponDetailResponse;
import com.example.promotionservice.dto.response.AdminCouponResponse;
import com.example.promotionservice.global.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdminCouponService {

    PageResponse<AdminCouponResponse> getCoupons(String keyword, String status, Pageable pageable);

    AdminCouponDetailResponse getCouponDetail(Long couponId);

    AdminCouponDetailResponse createCoupon(AdminCouponCreateRequest request);

    AdminCouponDetailResponse updateCoupon(Long couponId, AdminCouponUpdateRequest request);
}

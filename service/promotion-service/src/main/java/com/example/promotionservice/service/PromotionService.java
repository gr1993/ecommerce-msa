package com.example.promotionservice.service;

import com.example.promotionservice.dto.request.CouponClaimRequest;
import com.example.promotionservice.dto.response.CouponClaimResponse;

public interface PromotionService {

    CouponClaimResponse claimCoupon(Long userId, CouponClaimRequest request);
}

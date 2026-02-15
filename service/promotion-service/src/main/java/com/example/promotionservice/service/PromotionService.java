package com.example.promotionservice.service;

import com.example.promotionservice.dto.request.CouponClaimRequest;
import com.example.promotionservice.dto.response.ApplicableDiscountPolicyResponse;
import com.example.promotionservice.dto.response.CouponClaimResponse;
import com.example.promotionservice.dto.response.UserCouponResponse;

import java.util.List;

public interface PromotionService {

    CouponClaimResponse claimCoupon(Long userId, CouponClaimRequest request);

    List<UserCouponResponse> getUserCoupons(Long userId);

    List<ApplicableDiscountPolicyResponse> getApplicableDiscountPolicies(List<Long> productIds);
}

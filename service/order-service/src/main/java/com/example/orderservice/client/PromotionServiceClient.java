package com.example.orderservice.client;

import com.example.orderservice.client.dto.ApplicableDiscountPolicyResponse;
import com.example.orderservice.client.dto.UserCouponResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "promotion-service", url = "${promotion-service.url}")
public interface PromotionServiceClient {

	@GetMapping("/api/promotion/coupons")
	List<UserCouponResponse> getUserCoupons(@RequestHeader("X-User-Id") Long userId);

	@GetMapping("/api/promotion/discount-policies")
	List<ApplicableDiscountPolicyResponse> getApplicableDiscountPolicies(
			@RequestParam("productIds") List<Long> productIds);
}

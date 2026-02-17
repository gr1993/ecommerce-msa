package com.example.orderservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponResponse {

	private Long userCouponId;
	private Long couponId;
	private String couponCode;
	private String couponName;
	private String discountType;
	private BigDecimal discountValue;
	private BigDecimal minOrderAmount;
	private BigDecimal maxDiscountAmount;
	private LocalDateTime validFrom;
	private LocalDateTime validTo;
	private String couponStatus;
	private LocalDateTime usedAt;
	private LocalDateTime issuedAt;
}

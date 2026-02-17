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
public class ApplicableDiscountPolicyResponse {

	private Long discountId;
	private String discountName;
	private String discountType;
	private BigDecimal discountValue;
	private String targetType;
	private Long targetId;
	private BigDecimal minOrderAmount;
	private BigDecimal maxDiscountAmount;
	private LocalDateTime validFrom;
	private LocalDateTime validTo;
}

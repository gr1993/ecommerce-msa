package com.example.orderservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRestoredEvent {

	private String orderId;
	private Long userCouponId;
	private LocalDateTime restoredAt;
}

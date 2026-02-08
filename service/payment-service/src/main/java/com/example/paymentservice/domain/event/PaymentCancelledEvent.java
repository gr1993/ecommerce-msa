package com.example.paymentservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelledEvent {

	private String orderId;
	private Long amount;
	private String customerId;
	private String cancelReason;
	private LocalDateTime cancelledAt;
}

package com.example.orderservice.consumer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmedEvent {

	private Long orderId;
	private String paymentKey;
	private String paymentMethod;
	private Long paymentAmount;
	private String paymentStatus;
	private String paidAt;
	private String customerId;
}

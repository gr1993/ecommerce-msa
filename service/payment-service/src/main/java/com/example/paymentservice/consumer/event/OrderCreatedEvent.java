package com.example.paymentservice.consumer.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
	private Long orderId;
	private String orderNumber;
	private Long userId;
	private String orderStatus;
	private BigDecimal totalProductAmount;
	private BigDecimal totalDiscountAmount;
	private BigDecimal totalPaymentAmount;
	private List<OrderItemSnapshot> orderItems;
	private DeliverySnapshot delivery;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
	private LocalDateTime orderedAt;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OrderItemSnapshot {
		private Long orderItemId;
		private Long productId;
		private Long skuId;
		private String productName;
		private String productCode;
		private Integer quantity;
		private BigDecimal unitPrice;
		private BigDecimal totalPrice;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DeliverySnapshot {
		private String receiverName;
		private String receiverPhone;
		private String zipcode;
		private String address;
		private String addressDetail;
		private String deliveryMemo;
	}

	/**
	 * 주문 이름 생성 (결제창 표시용)
	 * 예: "상품명 외 2개"
	 */
	public String generateOrderName() {
		if (orderItems == null || orderItems.isEmpty()) {
			return "주문";
		}
		String firstProductName = orderItems.get(0).getProductName();
		int additionalCount = orderItems.size() - 1;
		if (additionalCount > 0) {
			return firstProductName + " 외 " + additionalCount + "개";
		}
		return firstProductName;
	}
}

package com.example.orderservice.global.common;

public final class EventTypeConstants {

	private EventTypeConstants() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// Kafka Topics - Order
	public static final String TOPIC_ORDER_CREATED = "order.created";
	public static final String TOPIC_ORDER_CANCELLED = "order.cancelled";
	public static final String TOPIC_INVENTORY_DECREASE = "inventory.decrease";

	// Kafka Topics - Coupon
	public static final String TOPIC_COUPON_USED = "coupon.used";
	public static final String TOPIC_COUPON_RESTORED = "coupon.restored";

	// Kafka Type IDs (Consumer의 TYPE_MAPPINGS와 일치해야 함) - Order
	public static final String TYPE_ID_ORDER_CREATED = "orderCreatedEvent";
	public static final String TYPE_ID_ORDER_CANCELLED = "orderCancelledEvent";

	// Kafka Type IDs - Coupon
	public static final String TYPE_ID_COUPON_USED = "couponUsedEvent";
	public static final String TYPE_ID_COUPON_RESTORED = "couponRestoredEvent";
}

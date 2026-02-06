package com.example.orderservice.global.common;

public final class EventTypeConstants {

	private EventTypeConstants() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// Kafka Topics - Order
	public static final String TOPIC_ORDER_CREATED = "order.created";

	// Kafka Type IDs (Consumer의 TYPE_MAPPINGS와 일치해야 함) - Order
	public static final String TYPE_ID_ORDER_CREATED = "orderCreatedEvent";
}

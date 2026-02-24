package com.example.shippingservice.global.common;

public final class EventTypeConstants {

	private EventTypeConstants() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// Kafka Topics - Shipping
	public static final String TOPIC_SHIPPING_STARTED = "shipping.started";
	public static final String TOPIC_SHIPPING_DELIVERED = "shipping.delivered";

	// Kafka Topics - Return
	public static final String TOPIC_RETURN_APPROVED = "return.approved";
	public static final String TOPIC_RETURN_COMPLETED = "return.completed";

	// Kafka Type IDs (Consumer의 TYPE_MAPPINGS와 일치해야 함) - Shipping
	public static final String TYPE_ID_SHIPPING_STARTED = "shippingStartedEvent";
	public static final String TYPE_ID_SHIPPING_DELIVERED = "shippingDeliveredEvent";

	// Kafka Type IDs - Return
	public static final String TYPE_ID_RETURN_APPROVED = "returnApprovedEvent";
	public static final String TYPE_ID_RETURN_COMPLETED = "returnCompletedEvent";
}

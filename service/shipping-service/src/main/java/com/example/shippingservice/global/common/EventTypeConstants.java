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
	public static final String TOPIC_RETURN_IN_TRANSIT = "return.in-transit";
	public static final String TOPIC_RETURN_COMPLETED = "return.completed";

	// Kafka Topics - Exchange
	public static final String TOPIC_EXCHANGE_COLLECTING = "exchange.collecting";
	public static final String TOPIC_EXCHANGE_RETURN_COMPLETED = "exchange.return-completed";
	public static final String TOPIC_EXCHANGE_SHIPPING = "exchange.shipping";
	public static final String TOPIC_EXCHANGE_COMPLETED = "exchange.completed";

	// Kafka Type IDs (Consumer의 TYPE_MAPPINGS와 일치해야 함) - Shipping
	public static final String TYPE_ID_SHIPPING_STARTED = "shippingStartedEvent";
	public static final String TYPE_ID_SHIPPING_DELIVERED = "shippingDeliveredEvent";

	// Kafka Type IDs - Return
	public static final String TYPE_ID_RETURN_APPROVED = "returnApprovedEvent";
	public static final String TYPE_ID_RETURN_IN_TRANSIT = "returnInTransitEvent";
	public static final String TYPE_ID_RETURN_COMPLETED = "returnCompletedEvent";

	// Kafka Type IDs - Exchange
	public static final String TYPE_ID_EXCHANGE_COLLECTING = "exchangeCollectingEvent";
	public static final String TYPE_ID_EXCHANGE_RETURN_COMPLETED = "exchangeReturnCompletedEvent";
	public static final String TYPE_ID_EXCHANGE_SHIPPING = "exchangeShippingEvent";
	public static final String TYPE_ID_EXCHANGE_COMPLETED = "exchangeCompletedEvent";
}

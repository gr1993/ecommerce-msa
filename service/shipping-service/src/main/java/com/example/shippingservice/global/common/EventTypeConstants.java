package com.example.shippingservice.global.common;

public final class EventTypeConstants {

	private EventTypeConstants() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// Kafka Topics - Return
	public static final String TOPIC_RETURN_COMPLETED = "return.completed";

	// Kafka Type IDs (Consumer의 TYPE_MAPPINGS와 일치해야 함) - Return
	public static final String TYPE_ID_RETURN_COMPLETED = "returnCompletedEvent";
}

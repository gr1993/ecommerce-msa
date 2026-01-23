package com.example.productservice.global.common;

public final class EventTypeConstants {

	private EventTypeConstants() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// Kafka Topics
	public static final String TOPIC_PRODUCT_CREATED = "product.created";
	public static final String TOPIC_PRODUCT_UPDATED = "product.updated";
}

package com.example.productservice.global.common;

public final class EventTypeConstants {

	private EventTypeConstants() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// Kafka Topics
	public static final String TOPIC_PRODUCT_CREATED = "product.created";
	public static final String TOPIC_PRODUCT_UPDATED = "product.updated";

	// Kafka Type IDs (Consumer의 TYPE_MAPPINGS와 일치해야 함)
	public static final String TYPE_ID_PRODUCT_CREATED = "productCreatedEvent";
	public static final String TYPE_ID_PRODUCT_UPDATED = "productUpdatedEvent";
}

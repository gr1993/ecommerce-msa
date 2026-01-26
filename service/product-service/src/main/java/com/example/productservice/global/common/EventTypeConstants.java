package com.example.productservice.global.common;

public final class EventTypeConstants {

	private EventTypeConstants() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// Kafka Topics - Product
	public static final String TOPIC_PRODUCT_CREATED = "product.created";
	public static final String TOPIC_PRODUCT_UPDATED = "product.updated";

	// Kafka Topics - Category
	public static final String TOPIC_CATEGORY_CREATED = "category.created";
	public static final String TOPIC_CATEGORY_UPDATED = "category.updated";
	public static final String TOPIC_CATEGORY_DELETED = "category.deleted";

	// Kafka Type IDs (Consumer의 TYPE_MAPPINGS와 일치해야 함) - Product
	public static final String TYPE_ID_PRODUCT_CREATED = "productCreatedEvent";
	public static final String TYPE_ID_PRODUCT_UPDATED = "productUpdatedEvent";

	// Kafka Type IDs (Consumer의 TYPE_MAPPINGS와 일치해야 함) - Category
	public static final String TYPE_ID_CATEGORY_CREATED = "categoryCreatedEvent";
	public static final String TYPE_ID_CATEGORY_UPDATED = "categoryUpdatedEvent";
	public static final String TYPE_ID_CATEGORY_DELETED = "categoryDeletedEvent";
}

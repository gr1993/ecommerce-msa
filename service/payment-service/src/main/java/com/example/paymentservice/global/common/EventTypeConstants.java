package com.example.paymentservice.global.common;

public final class EventTypeConstants {

	private EventTypeConstants() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// Kafka Topics - Payment
	public static final String TOPIC_PAYMENT_CANCELLED = "payment.cancelled";
	public static final String TOPIC_PAYMENT_CONFIRMED = "payment.confirmed";

	// Kafka Type IDs
	public static final String TYPE_ID_PAYMENT_CANCELLED = "paymentCancelledEvent";
	public static final String TYPE_ID_PAYMENT_CONFIRMED = "paymentConfirmedEvent";
}

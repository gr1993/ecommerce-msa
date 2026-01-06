package com.example.authservice.common;

public final class EventTypeConstants {

	private EventTypeConstants() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// Kafka Topics
	public static final String TOPIC_USER_REGISTERED = "user.registered";
	public static final String TOPIC_USER_UPDATED = "user.updated";
}

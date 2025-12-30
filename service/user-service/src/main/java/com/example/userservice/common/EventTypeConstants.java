package com.example.userservice.common;

public final class EventTypeConstants {

	private EventTypeConstants() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// Kafka Topics (CamelCase를 소문자로 변환하고 단어 사이에 . 추가)
	public static final String TOPIC_USER_REGISTERED = "user.registered";
    public static final String TOPIC_USER_UPDATED = "user.updated";
}


package com.example.authservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
	private Long userId;
	private String email;
	private String name;
	private String phone;
	private String hashedPassword;
	private LocalDateTime registeredAt;
}

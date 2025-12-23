package com.example.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SignUpResponse {
	private Long userId;
	private String email;
	private String name;
	private String phone;
	private LocalDateTime createdAt;
}


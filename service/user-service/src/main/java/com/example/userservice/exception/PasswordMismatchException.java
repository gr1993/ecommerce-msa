package com.example.userservice.exception;

public class PasswordMismatchException extends RuntimeException {
	public PasswordMismatchException(String message) {
		super(message);
	}
}



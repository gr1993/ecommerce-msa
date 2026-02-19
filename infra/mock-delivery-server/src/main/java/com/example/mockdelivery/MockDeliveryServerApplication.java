package com.example.mockdelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MockDeliveryServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MockDeliveryServerApplication.class, args);
	}

}

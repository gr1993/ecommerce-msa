package com.example.paymentservice;

import com.example.paymentservice.client.TossPaymentsClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"springwolf.enabled=false",
		"toss.payments.api.base-url=https://api.tosspayments.com",
		"toss.payments.api.secret-key=test_sk_test"
})
class PaymentServiceApplicationTests {

	@MockitoBean
	private TossPaymentsClient tossPaymentsClient;

	@Test
	void contextLoads() {
	}

}

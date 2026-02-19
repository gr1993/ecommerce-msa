package com.example.mockdelivery.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mock Delivery API")
                        .description("Mock 배송 API 문서")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Mock Delivery Team")
                                .email("mock-delivery@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }

    @Bean
    public GroupedOpenApi mockDeliveryApi() {
        return GroupedOpenApi.builder()
                .group("Mock-delivery")
                .packagesToScan("com.example.mockdelivery")
                .pathsToMatch("/**")
                .build();
    }
}

package com.example.promotionservice.config;

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
                        .title("Promotion Service API")
                        .description("프로모션 관리 서비스 API 문서")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Promotion Service Team")
                                .email("Promotion-service@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }

    @Bean
    public GroupedOpenApi promotionApi() {
        return GroupedOpenApi.builder()
                .group("Promotion-service")
                .packagesToScan("com.example.promotionservice")
                .pathsToMatch("/**")
                .build();
    }
}
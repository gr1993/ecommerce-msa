package com.example.gatewayservice.filter;

import com.example.gatewayservice.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtAuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationGatewayFilterFactory(JwtTokenProvider jwtTokenProvider) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Authorization 헤더에서 토큰 추출
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header");
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7); // "Bearer " 제거

            try {
                // 토큰 검증
                Claims claims = jwtTokenProvider.validateToken(token);

                // 검증된 사용자 정보를 헤더에 추가하여 다운스트림 서비스로 전달
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", claims.getSubject())
                        .header("X-User-Email", claims.get("email", String.class))
                        .build();

                ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(modifiedRequest)
                        .build();

                log.info("Successfully authenticated user: {}", claims.getSubject());
                return chain.filter(modifiedExchange);

            } catch (ExpiredJwtException e) {
                log.warn("Expired JWT token: {}", e.getMessage());
                return onError(exchange, "Token expired", HttpStatus.UNAUTHORIZED);
            } catch (JwtException e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                log.error("Authentication error: {}", e.getMessage(), e);
                return onError(exchange, "Authentication failed", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String errorResponse = String.format("{\"error\": \"%s\", \"message\": \"%s\"}",
                status.getReasonPhrase(), message);

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(errorResponse.getBytes()))
        );
    }

    public static class Config {
        // 필요한 경우 필터 설정 추가 가능
    }
}

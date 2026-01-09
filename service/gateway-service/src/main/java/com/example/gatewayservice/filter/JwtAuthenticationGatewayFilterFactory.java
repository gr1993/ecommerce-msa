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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("requiredRole");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String requestPath = request.getURI().getPath();

            // Step 1: Check if path is whitelisted
            if (isWhitelisted(requestPath, config.getWhitelist())) {
                log.info("Request to whitelisted path: {}", requestPath);
                return chain.filter(exchange);
            }

            // Step 2: Extract Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for path: {}", requestPath);
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7); // "Bearer " 제거

            try {
                // Step 3: Validate token and extract claims
                Claims claims = jwtTokenProvider.validateToken(token);
                String userId = claims.getSubject();
                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);

                log.debug("Extracted claims - userId: {}, email: {}, role: {}", userId, email, role);

                // Step 4: Validate role authorization
                if (!hasRequiredRole(role, config.getRequiredRole())) {
                    log.warn("Access denied for user {} with role {} to path {} (required: {})",
                            userId, role, requestPath, config.getRequiredRole());
                    return onError(exchange, "Insufficient permissions", HttpStatus.FORBIDDEN);
                }

                // Step 5: Add validated user info to headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Email", email)
                        .header("X-User-Role", role != null ? role : "USER")
                        .build();

                ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(modifiedRequest)
                        .build();

                log.info("Successfully authenticated user: {} with role: {} for path: {}",
                        userId, role, requestPath);
                return chain.filter(modifiedExchange);

            } catch (ExpiredJwtException e) {
                log.warn("Expired JWT token for path {}: {}", requestPath, e.getMessage());
                return onError(exchange, "Token expired", HttpStatus.UNAUTHORIZED);
            } catch (JwtException e) {
                log.warn("Invalid JWT token for path {}: {}", requestPath, e.getMessage());
                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                log.error("Authentication error for path {}: {}", requestPath, e.getMessage(), e);
                return onError(exchange, "Authentication failed", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    /**
     * 요청 경로가 화이트리스트에 포함되는지 확인합니다.
     *
     * @param requestPath 요청 경로
     * @param whitelist 화이트리스트 경로 목록
     * @return 화이트리스트 포함 여부
     */
    private boolean isWhitelisted(String requestPath, List<String> whitelist) {
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }

        for (String whitelistedPath : whitelist) {
            // Exact match
            if (requestPath.equals(whitelistedPath)) {
                return true;
            }

            // Wildcard support: /api/auth/register* matches /api/auth/register and /api/auth/register/confirm
            if (whitelistedPath.endsWith("*")) {
                String basePattern = whitelistedPath.substring(0, whitelistedPath.length() - 1);
                if (requestPath.startsWith(basePattern)) {
                    return true;
                }
            }

            // Double wildcard support: /api/auth/** matches any sub-path
            if (whitelistedPath.endsWith("/**")) {
                String basePattern = whitelistedPath.substring(0, whitelistedPath.length() - 3);
                if (requestPath.startsWith(basePattern)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 사용자 역할이 요구되는 역할을 만족하는지 검증합니다.
     * ADMIN은 모든 역할에 접근 가능하고, USER는 USER 역할에만 접근 가능합니다.
     *
     * @param userRole 사용자의 역할
     * @param requiredRole 요구되는 역할
     * @return 접근 가능 여부
     */
    private boolean hasRequiredRole(String userRole, String requiredRole) {
        // No role requirement - allow all authenticated users
        if (requiredRole == null || requiredRole.isEmpty()) {
            return true;
        }

        // User role is null or empty - deny access
        if (userRole == null || userRole.isEmpty()) {
            return false;
        }

        // ADMIN can access everything (role hierarchy)
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            return true;
        }

        // USER can only access USER-level endpoints
        if ("USER".equalsIgnoreCase(userRole)) {
            return "USER".equalsIgnoreCase(requiredRole);
        }

        // Unknown role - deny access
        return false;
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
        private String requiredRole;
        private List<String> whitelist = new ArrayList<>();

        public String getRequiredRole() {
            return requiredRole;
        }

        public void setRequiredRole(String requiredRole) {
            this.requiredRole = requiredRole;
        }

        public List<String> getWhitelist() {
            return whitelist;
        }

        public void setWhitelist(List<String> whitelist) {
            this.whitelist = whitelist != null ? whitelist : new ArrayList<>();
        }
    }
}

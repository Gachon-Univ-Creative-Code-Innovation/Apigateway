package com.gucci.apigatewayservice.filter;

import com.gucci.apigatewayservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthorizationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        log.info("Incoming request path: {}", path);

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null) {
            return onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
        }

        if (!authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Authorization header must start with 'Bearer '", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.replace("Bearer ", "").trim();

        if (!jwtUtil.isValidToken(token)) {
            return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
        }

        Claims claims = jwtUtil.extractClaims(token);
        log.info("Authenticated user: email={}, user_id={}, role={}",
                claims.getSubject(),
                claims.get("user_id"),
                claims.get("role")
        );

        return chain.filter(exchange);
    }

    // List of public paths that do not require JWT authentication
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/user-service/signin",
            "/api/user-service/signup",
            "/api/user-service/verify",
            "/api/user-service/health-check",
            "/api/user-service/check-email",
            "/api/user-service/check-nickname",
            "/api/user-service/google",
            "/api/user-service/kakao",
            "/api/user-service/refresh-token",
            "/api/user-service/reset-password",
            "/health-check",
            "/api/summarize-service/health-check",
            "/api/matching-service/health-check",
            "/api/alarm-service/health-check",
            "/api/blog-service/posts/trending",
            "/swagger/blog-service/swagger-ui/",
            "/swagger/blog-service/swagger-ui/index.html",
            "/swagger/blog-service/v3/api-docs",
            "/api/github-service/health-check",
            "/api/user-service/details",
            "/api/user-service/profile-nickname",
            "/api/blog-service/health-check",
            "/api/portfolio-service/health-check",
            "/api/message-service/health-check",
            "/api/roadmap/health-check",
            "/api/alarm-service/notifications/subscribe",
            "/api/blog-service/comment/",
            "/api/blog-service/posts/",
            "/api/blog-service/comments/",
            "/api/blog-service/posts/user/"


    );

    private boolean isPublic(String path) {
        if (path.startsWith("/ws/") || path.equals("/ws")) {
            return true;
        }
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        log.error("JWT Filter Error: {}", message);

        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String json = String.format(
                "{\"status\": %d, \"error\": \"%s\", \"message\": \"%s\"}",
                status.value(),
                status.getReasonPhrase(),
                message
        );

        var buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
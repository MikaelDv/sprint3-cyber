package br.com.challenge2026.challengeFord.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    @Value("${security.rate-limit.capacity}")
    private long capacity;

    @Value("${security.rate-limit.refill-tokens}")
    private long refillTokens;

    @Value("${security.rate-limit.refill-period-seconds}")
    private long refillSeconds;

    @Value("${security.rate-limit.login-capacity}")
    private long loginCapacity;

    @Value("${security.rate-limit.login-refill-period-seconds}")
    private long loginRefillSeconds;

    private Bucket resolveGeneric(String key) {
        return buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(refillTokens, Duration.ofSeconds(refillSeconds))
                        .build())
                .build());
    }

    private Bucket resolveLogin(String key) {
        return loginBuckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(loginCapacity)
                        .refillIntervally(loginCapacity, Duration.ofSeconds(loginRefillSeconds))
                        .build())
                .build());
    }

    private String clientKey(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr() == null ? "unknown" : req.getRemoteAddr();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String key = clientKey(request);
        Bucket bucket = path != null && path.startsWith("/auth/")
                ? resolveLogin(key)
                : resolveGeneric(key);

        if (bucket.tryConsume(1)) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(refillSeconds));
            response.setContentType("application/json");
            response.getWriter().write("{\"erro\":\"Limite de requisições excedido\",\"status\":429}");
        }
    }
}

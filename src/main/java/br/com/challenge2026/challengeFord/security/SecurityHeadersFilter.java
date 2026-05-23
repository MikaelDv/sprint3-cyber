package br.com.challenge2026.challengeFord.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank() || requestId.length() > 64) {
            requestId = UUID.randomUUID().toString();
        }
        org.slf4j.MDC.put("requestId", requestId);
        try {
            response.setHeader(REQUEST_ID_HEADER, requestId);
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("Referrer-Policy", "no-referrer");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
            response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            response.setHeader("Server", "api");
            chain.doFilter(request, response);
        } finally {
            org.slf4j.MDC.remove("requestId");
        }
    }
}

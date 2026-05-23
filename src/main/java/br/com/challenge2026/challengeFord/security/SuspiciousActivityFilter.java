package br.com.challenge2026.challengeFord.security;

import br.com.challenge2026.challengeFord.util.LogSanitizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Component
public class SuspiciousActivityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("security");

    private static final Pattern SUSPICIOUS = Pattern.compile(
            "(?i)(union\\s+select|<script|\\.\\./|/etc/passwd|drop\\s+table|\\bxp_cmdshell\\b|`|\\$\\{jndi)"
    );

    private final ConcurrentMap<String, AtomicInteger> auth401 = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        if (uri != null && SUSPICIOUS.matcher(uri).find()) {
            log.warn("payload suspeito em path={} ip={}",
                    LogSanitizer.safe(uri), LogSanitizer.safe(request.getRemoteAddr()));
        }
        if (query != null && SUSPICIOUS.matcher(query).find()) {
            log.warn("payload suspeito em query ip={}", LogSanitizer.safe(request.getRemoteAddr()));
        }

        chain.doFilter(request, response);

        int status = response.getStatus();
        if (status == 401 || status == 403) {
            String key = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
            int total = auth401.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
            if (total > 0 && total % 5 == 0) {
                log.warn("Possível brute-force ip={} total={} path={}",
                        LogSanitizer.safe(key), total, LogSanitizer.safe(uri));
            }
        }
    }
}

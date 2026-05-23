package br.com.challenge2026.challengeFord.security;

import br.com.challenge2026.challengeFord.crypto.HmacSigner;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Component
public class HmacSignatureFilter extends OncePerRequestFilter {

    private final HmacSigner signer;

    @Value("${security.hmac.required-paths}")
    private String requiredPathsCsv;

    @Value("${security.hmac.header}")
    private String signatureHeader;

    @Value("${security.hmac.timestamp-header}")
    private String timestampHeader;

    @Value("${security.hmac.max-skew-seconds}")
    private long maxSkewSeconds;

    private List<String> requiredPaths;

    public HmacSignatureFilter(HmacSigner signer) {
        this.signer = signer;
    }

    @Override
    protected void initFilterBean() {
        this.requiredPaths = Arrays.stream(requiredPathsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private boolean requiresSignature(String path) {
        if (path == null) return false;
        return requiredPaths.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!requiresSignature(request.getRequestURI()) || !"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String signature = request.getHeader(signatureHeader);
        String timestamp = request.getHeader(timestampHeader);

        if (signature == null || timestamp == null) {
            reject(response, "Cabeçalhos de assinatura ausentes");
            return;
        }

        try {
            long ts = Long.parseLong(timestamp);
            long skew = Math.abs(Instant.now().getEpochSecond() - ts);
            if (skew > maxSkewSeconds) {
                reject(response, "Assinatura expirada");
                return;
            }
        } catch (NumberFormatException e) {
            reject(response, "Timestamp inválido");
            return;
        }

        CachedBodyRequest wrapper = new CachedBodyRequest(request);
        String body = new String(wrapper.cachedBody, StandardCharsets.UTF_8);
        if (!signer.verify(signature, timestamp, request.getMethod(), request.getRequestURI(), body)) {
            reject(response, "Assinatura inválida");
            return;
        }
        chain.doFilter(wrapper, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write("{\"erro\":\"" + message + "\",\"status\":400}");
    }

    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        final byte[] cachedBody;

        CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bis = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return bis.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener readListener) { }
                @Override public int read() { return bis.read(); }
            };
        }
    }
}

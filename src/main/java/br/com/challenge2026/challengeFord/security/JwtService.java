package br.com.challenge2026.challengeFord.security;

import br.com.challenge2026.challengeFord.model.Role;
import br.com.challenge2026.challengeFord.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String base64Secret;

    @Value("${security.jwt.access-token-ttl-minutes}")
    private long accessTtlMinutes;

    @Value("${security.jwt.issuer}")
    private String issuer;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret deve ter pelo menos 256 bits (32 bytes)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Usuario usuario) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessTtlMinutes, ChronoUnit.MINUTES);
        List<String> roles = usuario.getRoles().stream().map(Enum::name).toList();
        return Jwts.builder()
                .issuer(issuer)
                .subject(usuario.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(exp))
                .claim("roles", roles)
                .claim("uid", usuario.getId())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public long getAccessTtlSeconds() {
        return accessTtlMinutes * 60;
    }

    public Claims parseAndValidate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Token inválido");
        }
    }

    @SuppressWarnings("unchecked")
    public Set<Role> extractRoles(Claims claims) {
        List<String> raw = (List<String>) claims.getOrDefault("roles", List.of());
        return raw.stream()
                .map(r -> {
                    try { return Role.valueOf(r); }
                    catch (Exception e) { return null; }
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }
}

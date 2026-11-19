package com.eventhub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration}") Duration expiration) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret precisa ter pelo menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiration = expiration;
    }

    public record TokenClaims(Long userId, Long tenantId) {
    }

    public String generate(Long userId, Long tenantId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tid", tenantId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    public Optional<TokenClaims> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            Long tenantId = claims.get("tid", Long.class);
            if (tenantId == null) {
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(userId, tenantId));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long expirationSeconds() {
        return expiration.toSeconds();
    }
}

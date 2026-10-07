package com.example.app.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Issues and checks signed (HS256) tokens. The token itself carries who the user is, so no session is needed. */
@Service
public class JwtService {

    public static final Duration TOKEN_TTL = Duration.ofMinutes(15);

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret:}") String secret) {
        if (secret.isBlank()) {
            // Dev/test fallback: a random key per start, so tokens die on restart and nothing is hard-coded.
            log.warn("JWT_SECRET is not set; using a random signing key. Set JWT_SECRET (32+ bytes) in production.");
            this.key = Jwts.SIG.HS256.key().build();
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String createToken(AppUser user) {
        return createToken(user, Instant.now());
    }

    String createToken(AppUser user, Instant issuedAt) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(TOKEN_TTL)))
                .signWith(key)
                .compact();
    }

    /** Returns the claims, or throws JwtException if the token is tampered with, malformed or expired. */
    public Claims parse(String token) throws JwtException {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

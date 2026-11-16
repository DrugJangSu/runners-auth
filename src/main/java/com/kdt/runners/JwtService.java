package com.kdt.runners;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;                                  // ← 추가
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    public static final String CLAIM_TYPE = "tokenType";

    private final SecretKey key;
    private final long accessSeconds;
    private final long refreshSeconds;

    public JwtService(
            @Value("${kdt.jwt.secret}") String secret,
            @Value("${kdt.jwt.access-seconds}") long accessSeconds,
            @Value("${kdt.jwt.refresh-seconds}") long refreshSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessSeconds = accessSeconds;
        this.refreshSeconds = refreshSeconds;
    }

    public String createAccess(Long runnerId) {                          // ← 수정
        return compact(runnerId, TYPE_ACCESS, this.accessSeconds);       // ← 수정
    }

    public String createRefresh(Long runnerId) {                         // ← 수정
        return compact(runnerId, TYPE_REFRESH, this.refreshSeconds);     // ← 수정
    }

    public Claims parseAccess(String token) {
        return parseWithType(token, TYPE_ACCESS);
    }

    public Claims parseRefresh(String token) {
        return parseWithType(token, TYPE_REFRESH);
    }

    public String bearerValue(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new TokenFailedException();
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new TokenFailedException();
        }
        return token;
    }

    private String compact(Long runnerId, String tokenType, long seconds) {   // ← 수정
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(runnerId.toString())                                 // ← 수정
                .id(java.util.UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(seconds)))
                .signWith(this.key, Jwts.SIG.HS256)
                .compact();
    }

    private Claims parseWithType(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            throw new TokenFailedException();
        }
        try {
            Jws<Claims> jws =                                                 // ← 수정 (여기부터)
                    Jwts.parser().verifyWith(this.key).build().parseSignedClaims(token);
            if (!"HS256".equals(jws.getHeader().getAlgorithm())) {
                throw new TokenFailedException();
            }
            Claims claims = jws.getPayload();                                 // ← 수정 (여기까지)

            Object type = claims.get(CLAIM_TYPE);
            if (type == null || !expectedType.equals(type.toString())) {
                throw new TokenFailedException();
            }
            if (claims.getSubject() == null
                    || claims.getSubject().isBlank()
                    || claims.getExpiration() == null) {
                throw new TokenFailedException();
            }
            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new TokenFailedException();
        }
    }

    public long accessSeconds() {                                             // ← 추가
        return accessSeconds;
    }

    public long refreshSeconds() {                                            // ← 추가
        return refreshSeconds;
    }
}
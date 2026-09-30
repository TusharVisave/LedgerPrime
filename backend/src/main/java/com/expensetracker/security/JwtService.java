package com.expensetracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;

@Service
public class JwtService {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";

    private final SecretKey key;
    private final long accessExpirationMillis;
    private final long refreshExpirationMillis;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-expiration-minutes:15}") long accessExpirationMinutes,
                      @Value("${app.jwt.refresh-expiration-days:7}") long refreshExpirationDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMillis = accessExpirationMinutes * 60_000L;
        this.refreshExpirationMillis = refreshExpirationDays * 24 * 60 * 60_000L;
    }

    /** Short-lived signed JWT for the access cookie. */
    public String generateAccessToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpirationMillis)))
                .signWith(key)
                .compact();
    }

    /** Opaque random refresh token. Only its SHA-256 hash is stored in the DB. */
    public String generateRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public Long getUserIdFromAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    public boolean isAccessTokenValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Instant refreshExpiresAt() {
        return Instant.now().plusMillis(refreshExpirationMillis);
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMillis / 1000;
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpirationMillis / 1000;
    }
}

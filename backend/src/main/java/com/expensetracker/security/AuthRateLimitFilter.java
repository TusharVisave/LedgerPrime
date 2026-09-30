package com.expensetracker.security;

import com.expensetracker.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory token-bucket rate limiter for the auth endpoints
 * (brute-force protection on login/register/refresh).
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final long maxRequestsPerMinute;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(@Value("${app.auth.rate-limit-per-minute:60}") long maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = clientIp(request);
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(maxRequestsPerMinute));
        if (!bucket.tryConsume()) {
            ErrorResponse body = ErrorResponse.builder()
                    .timestamp(Instant.now())
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                    .message("Too many requests. Please try again later.")
                    .path(request.getRequestURI())
                    .build();
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }
        // Opportunistic cleanup to bound memory.
        if (buckets.size() > 10_000) {
            buckets.entrySet().removeIf(e -> e.getValue().isStale());
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Bucket {
        private final long capacity;
        private final AtomicLong tokens;
        private final AtomicLong lastRefillNanos = new AtomicLong(System.nanoTime());

        Bucket(long capacity) {
            this.capacity = capacity;
            this.tokens = new AtomicLong(capacity);
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos.get();
            long refillIntervalNanos = 60_000_000_000L / Math.max(capacity, 1);
            long add = elapsed / refillIntervalNanos;
            if (add > 0) {
                tokens.updateAndGet(t -> Math.min(capacity, t + add));
                lastRefillNanos.addAndGet(add * refillIntervalNanos);
            }
        }

        boolean isStale() {
            return System.nanoTime() - lastRefillNanos.get() > 3_600_000_000_000L;
        }
    }
}

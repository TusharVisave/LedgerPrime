package com.expensetracker.service;

import com.expensetracker.dto.*;
import com.expensetracker.entity.RefreshToken;
import com.expensetracker.entity.User;
import com.expensetracker.exception.ApiException;
import com.expensetracker.repository.RefreshTokenRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.security.JwtService;
import com.expensetracker.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** Failed logins before the account is locked. */
    private static final int MAX_FAILED_ATTEMPTS = 5;
    /** Lockout duration after the threshold is hit. */
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategorySeedService categorySeedService;

    @Value("${app.security.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookie-same-site:Lax}")
    private String cookieSameSite;

    public record TokenPair(String accessToken, String refreshToken) {}

    public record AuthResult(AuthResponse user, TokenPair tokens) {}

    @Transactional
    public AuthResult register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Email already registered");
        }
        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        categorySeedService.seedFor(user);
        AuthResponse response = toResponse(user);
        return new AuthResult(response, issueTokens(user));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // Same response as a wrong password: no user enumeration.
            throw ApiException.unauthorized("Invalid credentials");
        }
        assertNotLocked(user);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw ApiException.unauthorized("Invalid credentials");
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        return new AuthResult(toResponse(user), issueTokens(user));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw ApiException.unauthorized("Refresh token missing");
        }
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(jwtService.hashRefreshToken(rawRefreshToken))
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        if (!stored.isActive()) {
            // Reuse of a rotated-out (or expired) token: possible token theft.
            // Revoke every session for this user as a precaution.
            refreshTokenRepository.revokeAllByUserId(stored.getUser().getId());
            throw ApiException.unauthorized(
                    "Refresh token reuse detected. All sessions have been revoked; please log in again.");
        }
        // Rotation: revoke the presented token before issuing a new pair.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        User user = stored.getUser();
        return new AuthResult(toResponse(user), issueTokens(user));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(jwtService.hashRefreshToken(rawRefreshToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    /** Revokes every refresh token for the user — "log out all devices". */
    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        // Force re-login everywhere: the password just changed.
        // (revokeAllByUserId flushes pending changes first, so this save is safe.)
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public AuthResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
        return toResponse(user);
    }

    private void assertNotLocked(User user) {
        Instant lockedUntil = user.getLockedUntil();
        if (lockedUntil != null) {
            Instant now = Instant.now();
            if (lockedUntil.isAfter(now)) {
                long retryAfter = Math.max(lockedUntil.getEpochSecond() - now.getEpochSecond(), 1);
                long minutes = (retryAfter + 59) / 60;
                throw ApiException.tooManyRequests(
                        "Account locked due to too many failed login attempts. Try again in "
                                + minutes + " minute(s).",
                        retryAfter);
            }
            // Lock expired: start the counter fresh.
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCKOUT_DURATION));
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            throw ApiException.tooManyRequests(
                    "Account locked due to too many failed login attempts. Try again in 15 minutes.",
                    LOCKOUT_DURATION.toSeconds());
        }
        userRepository.save(user);
    }

    private TokenPair issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken();
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setTokenHash(jwtService.hashRefreshToken(refreshToken));
        stored.setExpiresAt(jwtService.refreshExpiresAt());
        refreshTokenRepository.save(stored);
        return new TokenPair(accessToken, refreshToken);
    }

    /** httpOnly cookies — tokens never go to localStorage. */
    public List<ResponseCookie> cookiesFor(TokenPair pair) {
        return List.of(
                CookieUtil.buildCookie(JwtService.ACCESS_COOKIE, pair.accessToken(),
                        Duration.ofSeconds(jwtService.getAccessExpirationSeconds()), "/", cookieSecure, cookieSameSite),
                CookieUtil.buildCookie(JwtService.REFRESH_COOKIE, pair.refreshToken(),
                        Duration.ofSeconds(jwtService.getRefreshExpirationSeconds()), "/api/auth", cookieSecure, cookieSameSite)
        );
    }

    public List<ResponseCookie> clearCookies() {
        return List.of(
                CookieUtil.clearCookie(JwtService.ACCESS_COOKIE, "/", cookieSecure, cookieSameSite),
                CookieUtil.clearCookie(JwtService.REFRESH_COOKIE, "/api/auth", cookieSecure, cookieSameSite)
        );
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(user.getId(), user.getName(), user.getEmail());
    }
}

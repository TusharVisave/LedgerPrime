package com.expensetracker.controller;

import com.expensetracker.dto.AuthResponse;
import com.expensetracker.dto.ChangePasswordRequest;
import com.expensetracker.dto.LoginRequest;
import com.expensetracker.dto.RegisterRequest;
import com.expensetracker.security.JwtService;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.AuthService;
import com.expensetracker.service.AuthService.AuthResult;
import com.expensetracker.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result = authService.register(request);
        return withCookies(result.user(), authService.cookiesFor(result.tokens()), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request);
        return withCookies(result.user(), authService.cookiesFor(result.tokens()), HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String raw = CookieUtil.getCookieValue(request, JwtService.REFRESH_COOKIE);
        AuthResult result = authService.refresh(raw);
        return withCookies(result.user(), authService.cookiesFor(result.tokens()), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        authService.logout(CookieUtil.getCookieValue(request, JwtService.REFRESH_COOKIE));
        return withCookies(Map.of("message", "Logged out"), authService.clearCookies(), HttpStatus.OK);
    }

    /** Revokes every session for the current user ("log out all devices"). */
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        authService.logoutAll(principal.getUserId());
        return withCookies(Map.of("message", "Logged out from all devices"),
                authService.clearCookies(), HttpStatus.OK);
    }

    /** Changes the password; requires the current password and revokes all sessions. */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUserId(), request);
        return withCookies(Map.of("message", "Password changed. Please log in again."),
                authService.clearCookies(), HttpStatus.OK);
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.me(principal.getUserId());
    }

    private <T> ResponseEntity<T> withCookies(T body, List<ResponseCookie> cookies, HttpStatus status) {
        var builder = ResponseEntity.status(status);
        for (ResponseCookie cookie : cookies) {
            builder.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return builder.body(body);
    }
}

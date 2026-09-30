package com.expensetracker;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthHardeningTest extends MockMvcBaseTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "short1",       // too short
            "password",     // no digit
            "12345678",     // no letter
            "pass1",        // too short, has both
            "        ",      // blank-ish (8 spaces)
    })
    void register_rejectsWeakPassword(String weakPassword) throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Weak","email":"%s","password":"%s"}""".formatted(email, weakPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void register_acceptsStrongPassword() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Strong","email":"%s","password":"Str0ngPass!"}""".formatted(email)))
                .andExpect(status().isCreated());
    }

    @Test
    void login_locksAccountAfterFiveFailedAttempts() throws Exception {
        String email = uniqueEmail();
        registerAndLogin(email);
        String badLogin = """
                {"email":"%s","password":"wrong-pass1"}""".formatted(email);

        // First four failures: 401.
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badLogin))
                    .andExpect(status().isUnauthorized());
        }
        // Fifth failure triggers the lockout: 429 with retry info.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badLogin))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("locked")));

        // Even the correct password is rejected while locked.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void refresh_reuseRevokesAllSessions() throws Exception {
        String email = uniqueEmail();
        List<Cookie> cookies = registerAndLogin(email);
        Cookie firstRefresh = cookieNamed(cookies, "refresh_token");

        // Rotate: first refresh succeeds and yields a new token.
        var rotated = mockMvc.perform(post("/api/auth/refresh").cookie(firstRefresh))
                .andExpect(status().isOk())
                .andReturn();
        Cookie secondRefresh = cookieNamed(extractCookies(rotated), "refresh_token");

        // Reusing the rotated-out token is treated as theft: 401...
        mockMvc.perform(post("/api/auth/refresh").cookie(firstRefresh))
                .andExpect(status().isUnauthorized());

        // ...and every session for the user is revoked, including the new token.
        mockMvc.perform(post("/api/auth/refresh").cookie(secondRefresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_requiresCurrentPasswordAndRevokesSessions() throws Exception {
        String email = uniqueEmail();
        List<Cookie> cookies = registerAndLogin(email);
        Cookie access = cookieNamed(cookies, "access_token");
        Cookie refresh = cookieNamed(cookies, "refresh_token");

        // Wrong current password -> 401.
        mockMvc.perform(post("/api/auth/change-password").cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"nope1234","newPassword":"NewPass123"}"""))
                .andExpect(status().isUnauthorized());

        // Weak new password -> 400.
        mockMvc.perform(post("/api/auth/change-password").cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"weak"}"""))
                .andExpect(status().isBadRequest());

        // Correct change -> 200, and the old refresh token no longer works.
        mockMvc.perform(post("/api/auth/change-password").cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"NewPass123"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized());

        // New password works for login.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"NewPass123"}""".formatted(email)))
                .andExpect(status().isOk());
    }

    @Test
    void logoutAll_revokesEverySession() throws Exception {
        String email = uniqueEmail();
        List<Cookie> cookies = registerAndLogin(email);
        Cookie access = cookieNamed(cookies, "access_token");

        // Second session via refresh rotation.
        var rotated = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(cookieNamed(cookies, "refresh_token")))
                .andExpect(status().isOk())
                .andReturn();
        Cookie secondRefresh = cookieNamed(extractCookies(rotated), "refresh_token");

        mockMvc.perform(post("/api/auth/logout-all").cookie(access))
                .andExpect(status().isOk());

        // Both sessions' refresh tokens are dead.
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(cookieNamed(cookies, "refresh_token")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").cookie(secondRefresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authCookies_areHttpOnly() throws Exception {
        String email = uniqueEmail();
        List<String> setCookies = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ck","email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeaders("Set-Cookie");
        org.assertj.core.api.Assertions.assertThat(setCookies)
                .allSatisfy(h -> org.assertj.core.api.Assertions.assertThat(h)
                        .contains("HttpOnly")
                        .contains("SameSite=Lax"));
    }
}

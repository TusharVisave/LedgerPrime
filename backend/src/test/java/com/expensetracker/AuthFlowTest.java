package com.expensetracker;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthFlowTest extends MockMvcBaseTest {

    @Test
    void register_setsHttpOnlyCookies() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Tushar","email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(header().stringValues("Set-Cookie",
                        org.hamcrest.Matchers.hasItems(
                                org.hamcrest.Matchers.containsString("access_token="),
                                org.hamcrest.Matchers.containsString("refresh_token="))))
                .andExpect(header().stringValues("Set-Cookie",
                        org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.containsString("HttpOnly"))));
    }

    @Test
    void register_rejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        registerAndLogin(email);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Dup","email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void register_rejectsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email","password":"123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_rejectsWrongPassword() throws Exception {
        String email = uniqueEmail();
        registerAndLogin(email);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrong-password"}""".formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullFlow_meRefreshLogout() throws Exception {
        String email = uniqueEmail();
        List<Cookie> cookies = registerAndLogin(email);
        Cookie access = cookieNamed(cookies, "access_token");
        Cookie refresh = cookieNamed(cookies, "refresh_token");

        // me with access cookie
        mockMvc.perform(get("/api/auth/me").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // me without cookie -> 401
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        // refresh rotates the refresh token
        var refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(refresh))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.email").value(email))
                        .andReturn();
        List<Cookie> rotated = extractCookies(refreshResult);
        Cookie newRefresh = cookieNamed(rotated, "refresh_token");

        // old refresh token is now revoked
        mockMvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized());

        // logout revokes the current refresh token and clears both cookies
        var logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .cookie(cookieNamed(rotated, "access_token"))
                        .cookie(newRefresh))
                .andExpect(status().isOk())
                .andReturn();
        List<String> cleared = logoutResult.getResponse().getHeaders("Set-Cookie");
        org.assertj.core.api.Assertions.assertThat(cleared)
                .anySatisfy(h -> org.assertj.core.api.Assertions.assertThat(h)
                        .startsWith("access_token=;"))
                .anySatisfy(h -> org.assertj.core.api.Assertions.assertThat(h)
                        .startsWith("refresh_token=;"));

        mockMvc.perform(post("/api/auth/refresh").cookie(newRefresh))
                .andExpect(status().isUnauthorized());
    }
}

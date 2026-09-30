package com.expensetracker;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class MockMvcBaseTest {

    @Autowired
    protected MockMvc mockMvc;

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.local";
    }

    /** Registers and logs in, returning the auth cookies (access_token + refresh_token). */
    protected List<Cookie> registerAndLogin(String email) throws Exception {
        String body = """
                {"name":"Test User","email":"%s","password":"password123"}""".formatted(email);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return extractCookies(result);
    }

    protected List<Cookie> extractCookies(MvcResult result) {
        List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
        List<Cookie> cookies = new ArrayList<>();
        for (String header : setCookies) {
            String[] parts = header.split(";", 2);
            String[] nameValue = parts[0].split("=", 2);
            cookies.add(new Cookie(nameValue[0].trim(),
                    nameValue.length > 1 ? nameValue[1].trim() : ""));
        }
        return cookies;
    }

    protected Cookie cookieNamed(List<Cookie> cookies, String name) {
        return cookies.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cookie not found: " + name));
    }
}

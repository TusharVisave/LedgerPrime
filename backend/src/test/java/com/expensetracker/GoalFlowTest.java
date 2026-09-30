package com.expensetracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end coverage for savings goals: create, list, allocate, update, delete.
 */
class GoalFlowTest extends MockMvcBaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void goalCrudAndAllocation() throws Exception {
        List<Cookie> cookies = registerAndLogin(uniqueEmail());
        Cookie access = cookieNamed(cookies, "access_token");

        // create
        String created = mockMvc.perform(post("/api/goals")
                        .cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Emergency fund","targetAmount":"50000.00","deadline":"2027-12-31"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Emergency fund"))
                .andExpect(jsonPath("$.progressPercent").value(0))
                .andReturn().getResponse().getContentAsString();
        long goalId = objectMapper.readTree(created).get("id").asLong();

        // allocate twice
        mockMvc.perform(post("/api/goals/" + goalId + "/allocate")
                        .cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":"10000.00","note":"first deposit"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocated").value("10000.00"))
                .andExpect(jsonPath("$.progressPercent").value(20))
                .andExpect(jsonPath("$.remaining").value("40000.00"));

        mockMvc.perform(post("/api/goals/" + goalId + "/allocate")
                        .cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":"5000.00"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(30));

        // list shows it
        mockMvc.perform(get("/api/goals").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // update
        mockMvc.perform(put("/api/goals/" + goalId)
                        .cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Emergency fund v2","targetAmount":"60000.00"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Emergency fund v2"));

        // validation: negative target rejected
        mockMvc.perform(post("/api/goals")
                        .cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bad","targetAmount":"-5.00"}"""))
                .andExpect(status().isBadRequest());

        // delete
        mockMvc.perform(delete("/api/goals/" + goalId).cookie(access))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/goals/" + goalId).cookie(access))
                .andExpect(status().isNotFound());
    }

    @Test
    void goalsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isUnauthorized());
    }
}

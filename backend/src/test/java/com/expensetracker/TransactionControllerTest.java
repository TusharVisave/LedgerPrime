package com.expensetracker;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransactionControllerTest extends MockMvcBaseTest {

    private List<Cookie> cookies;

    @BeforeEach
    void login() throws Exception {
        cookies = registerAndLogin(uniqueEmail());
    }

    private String txBody(String type, String amount, String category, String note, String date) {
        return """
                {"type":"%s","amount":"%s","category":"%s","note":"%s","date":"%s","tags":["t1"]}"""
                .formatted(type, amount, category, note, date);
    }

    @Test
    void create_validTransaction_returns201WithPaiseMath() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txBody("expense", "1250.50", "Food", "lunch", "2026-09-15")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction.amount").value("1250.50"))
                .andExpect(jsonPath("$.transaction.amountPaise").value(125050))
                .andExpect(jsonPath("$.transaction.type").value("expense"))
                .andExpect(jsonPath("$.budgetStatus").exists());
    }

    @Test
    void create_rejectsNegativeAmount() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txBody("expense", "-5", "Food", "x", "2026-09-15")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void create_rejectsNonNumericAmount() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txBody("expense", "abc", "Food", "x", "2026-09-15")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void create_rejectsZeroAndThreeDecimalAmounts() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txBody("expense", "0", "Food", "x", "2026-09-15")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txBody("expense", "10.999", "Food", "x", "2026-09-15")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_rejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"expense","amount":"10.00"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.category").exists())
                .andExpect(jsonPath("$.fieldErrors.date").exists());
    }

    @Test
    void unauthenticated_requestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_paginatesAndRejectsOversizePages() throws Exception {
        for (int i = 1; i <= 30; i++) {
            mockMvc.perform(post("/api/transactions")
                            .cookie(cookies.toArray(new Cookie[0]))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(txBody("expense", "10.00", "Food", "n" + i,
                                    "2026-09-" + String.format("%02d", (i % 28) + 1))))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(get("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(30))
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalPages").value(3));

        mockMvc.perform(get("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_keywordWithWildcardsDoesNotMatchEverything() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txBody("expense", "10.00", "Food", "plain note", "2026-09-15")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .param("keyword", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void updateAndDelete_roundTrip() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txBody("expense", "100.00", "Food", "orig", "2026-09-15")))
                .andExpect(status().isCreated())
                .andReturn();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.transaction.id")).longValue();

        mockMvc.perform(put("/api/transactions/" + id)
                        .cookie(cookies.toArray(new Cookie[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txBody("income", "200.00", "Salary", "updated", "2026-09-16")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.amount").value("200.00"))
                .andExpect(jsonPath("$.transaction.type").value("income"));

        mockMvc.perform(delete("/api/transactions/" + id)
                        .cookie(cookies.toArray(new Cookie[0])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction deleted"));
    }

    @Test
    void update_otherUsersTransaction_returns404() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txBody("expense", "100.00", "Food", "mine", "2026-09-15")))
                .andExpect(status().isCreated())
                .andReturn();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.transaction.id")).longValue();

        List<Cookie> otherCookies = registerAndLogin(uniqueEmail());
        mockMvc.perform(put("/api/transactions/" + id)
                        .cookie(otherCookies.toArray(new Cookie[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txBody("expense", "1.00", "Food", "hijack", "2026-09-15")))
                .andExpect(status().isNotFound());
    }

    @Test
    void moneyMath_endToEnd_noFloatingPointError() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txBody("expense", "0.10", "Food", "a", "2026-09-15")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/transactions")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txBody("expense", "0.20", "Food", "b", "2026-09-15")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dashboard")
                        .cookie(cookies.toArray(new Cookie[0]))
                        .param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.expense").value("0.30"));
    }
}

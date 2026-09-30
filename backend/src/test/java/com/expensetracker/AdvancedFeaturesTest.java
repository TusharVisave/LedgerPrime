package com.expensetracker;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Coverage for the advanced feature endpoints: natural-language quick entry,
 * CSV import, subscriptions, anomalies, the AI report 501 path, and
 * per-category budgets.
 */
class AdvancedFeaturesTest extends MockMvcBaseTest {

    @Test
    void naturalLanguageParse() throws Exception {
        List<Cookie> cookies = registerAndLogin(uniqueEmail());
        Cookie access = cookieNamed(cookies, "access_token");

        mockMvc.perform(post("/api/transactions/parse")
                        .cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"spent 500 on lunch today"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("expense"))
                .andExpect(jsonPath("$.amount").value("500.00"))
                .andExpect(jsonPath("$.date").value(LocalDate.now().toString()));

        mockMvc.perform(post("/api/transactions/parse")
                        .cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":""}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void csvImportWithRowErrors() throws Exception {
        List<Cookie> cookies = registerAndLogin(uniqueEmail());
        Cookie access = cookieNamed(cookies, "access_token");

        String csv = """
                date,description,amount,type,category,note
                2026-09-01,Grocery store,1200.50,expense,Food,weekly shop
                2026-09-02,Salary,50000.00,income,Salary,
                not-a-date,Bad row,abc,expense,Food,
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/transactions/import")
                        .file(file)
                        .cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].row").value(4));

        // the imported transactions are visible in search
        mockMvc.perform(get("/api/transactions").cookie(access)
                        .param("keyword", "Grocery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void subscriptionsDerivedFromRecurringReminders() throws Exception {
        List<Cookie> cookies = registerAndLogin(uniqueEmail());
        Cookie access = cookieNamed(cookies, "access_token");

        String due = LocalDate.now().plusDays(3).toString();
        mockMvc.perform(post("/api/reminders")
                        .cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Netflix","amount":"649.00","category":"Entertainment",
                                 "dueDate":"%s","isRecurring":true,"recurrence":"monthly"}""".formatted(due)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/subscriptions").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.subscriptions[0].title").value("Netflix"))
                .andExpect(jsonPath("$.totalMonthlyCost").value("649.00"));
    }

    @Test
    void anomaliesEndpointAndAiReportNotConfigured() throws Exception {
        List<Cookie> cookies = registerAndLogin(uniqueEmail());
        Cookie access = cookieNamed(cookies, "access_token");

        mockMvc.perform(get("/api/insights/anomalies").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anomalies").isArray());

        // no API key in test config -> 501 with a clear message
        mockMvc.perform(post("/api/insights/ai-report")
                        .cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.message", containsString("not configured")));
    }

    @Test
    void categoryBudgetsEndpoint() throws Exception {
        List<Cookie> cookies = registerAndLogin(uniqueEmail());
        Cookie access = cookieNamed(cookies, "access_token");

        String month = LocalDate.now().toString().substring(0, 7);
        mockMvc.perform(put("/api/budgets")
                        .cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"month":"%s","amount":"8000.00","category":"Food","rollover":true}"""
                                .formatted(month)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/budgets/categories").cookie(access)
                        .param("month", month))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category").value("Food"))
                .andExpect(jsonPath("$[0].rollover").value(true));
    }

    @Test
    void receiptScanWithoutTesseractDegradesGracefully() throws Exception {
        List<Cookie> cookies = registerAndLogin(uniqueEmail());
        Cookie access = cookieNamed(cookies, "access_token");

        // 1x1 PNG — OCR will fail/degrade in this environment; the endpoint
        // must still store the image and return a usable response shape.
        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
                (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
                0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF,
                (byte) 0xC0, 0x00, 0x00, 0x00, 0x03, 0x00, 0x01,
                0x00, 0x05, (byte) 0xFE, (byte) 0xD8, (byte) 0x8E,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82};
        MockMultipartFile image = new MockMultipartFile(
                "image", "receipt.png", "image/png", png);

        mockMvc.perform(multipart("/api/receipts/scan")
                        .file(image)
                        .cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storedImage").isNotEmpty())
                .andExpect(jsonPath("$.ocrAvailable").isBoolean());
    }
}

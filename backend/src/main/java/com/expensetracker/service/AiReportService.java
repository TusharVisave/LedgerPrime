package com.expensetracker.service;

import com.expensetracker.dto.AiReportResponse;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.TransactionRepository.CategoryTotal;
import com.expensetracker.util.MoneyUtil;
import com.expensetracker.util.MonthUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * AI monthly report via an OpenAI-compatible chat-completions API
 * (NVIDIA NIM style: base URL + model + API key, all configurable).
 * BYOK: when no API key is configured the endpoint returns 501.
 * The key is never logged or returned. The core app works fully without it.
 */
@Service
@RequiredArgsConstructor
public class AiReportService {

    private static final Logger log = LoggerFactory.getLogger(AiReportService.class);

    private final TransactionRepository transactionRepository;

    @Value("${app.ai.base-url:https://integrate.api.nvidia.com/v1}")
    private String baseUrl;

    @Value("${app.ai.model:}")
    private String model;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.timeout-ms:20000}")
    private int timeoutMs;

    @Transactional(readOnly = true)
    public AiReportResponse generate(Long userId, String month) {
        if (apiKey == null || apiKey.isBlank()) {
            throw com.expensetracker.exception.ApiException.notImplemented(
                    "AI monthly report is not configured. Set app.ai.api-key (and optionally " +
                    "app.ai.base-url / app.ai.model) to enable it.");
        }
        if (model == null || model.isBlank()) {
            throw com.expensetracker.exception.ApiException.notImplemented(
                    "AI monthly report is not configured. Set app.ai.model to enable it.");
        }

        String key = (month == null || month.isBlank()) ? MonthUtil.currentMonthKey() : month.strip();
        LocalDate[] range;
        try {
            range = MonthUtil.monthRange(key);
        } catch (java.time.format.DateTimeParseException e) {
            throw com.expensetracker.exception.ApiException.badRequest(
                    "month must be in yyyy-MM format");
        }
        long income = transactionRepository.sumByUserIdAndTypeAndDateBetween(
                userId, TransactionType.INCOME, range[0], range[1]);
        long expense = transactionRepository.sumByUserIdAndTypeAndDateBetween(
                userId, TransactionType.EXPENSE, range[0], range[1]);
        List<CategoryTotal> byCategory = transactionRepository.sumByCategory(
                userId, TransactionType.EXPENSE, range[0], range[1]);

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a concise personal-finance assistant. Write a short monthly ")
              .append("spending report (bullets, under 200 words) for month ").append(key).append(".\n")
              .append("Total income: ").append(MoneyUtil.toDecimalString(income)).append("\n")
              .append("Total expenses: ").append(MoneyUtil.toDecimalString(expense)).append("\n")
              .append("Net savings: ").append(MoneyUtil.toDecimalString(income - expense)).append("\n")
              .append("Spending by category:\n");
        for (CategoryTotal row : byCategory) {
            prompt.append("- ").append(row.getName()).append(": ")
                  .append(MoneyUtil.toDecimalString(row.getTotal())).append("\n");
        }
        prompt.append("Give 2-3 practical observations and one actionable saving tip.");

        try {
            org.springframework.http.client.SimpleClientHttpRequestFactory rf =
                    new org.springframework.http.client.SimpleClientHttpRequestFactory();
            rf.setConnectTimeout(timeoutMs);
            rf.setReadTimeout(timeoutMs);
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .requestFactory(rf)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .build();

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt.toString())),
                    "max_tokens", 600,
                    "temperature", 0.4);

            Map<?, ?> response = client.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            String report = extractContent(response);
            if (report == null || report.isBlank()) {
                throw com.expensetracker.exception.ApiException.badGateway(
                        "AI provider returned an empty report.");
            }
            return AiReportResponse.builder().month(key).report(report.trim()).build();
        } catch (com.expensetracker.exception.ApiException e) {
            throw e;
        } catch (Exception e) {
            // Never include the key or its prefix in logs.
            log.warn("AI report request failed: {}", e.toString());
            throw com.expensetracker.exception.ApiException.badGateway(
                    "AI report request failed: " + rootMessage(e));
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> response) {
        if (response == null) {
            return null;
        }
        Object choices = response.get("choices");
        if (choices instanceof List<?> list && !list.isEmpty()
                && list.get(0) instanceof Map<?, ?> first) {
            Object message = first.get("message");
            if (message instanceof Map<?, ?> messageMap) {
                Object content = messageMap.get("content");
                return content == null ? null : content.toString();
            }
        }
        return null;
    }

    private String rootMessage(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null ? current.toString() : message;
    }
}

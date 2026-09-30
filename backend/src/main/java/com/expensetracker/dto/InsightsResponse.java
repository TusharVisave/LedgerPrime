package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class InsightsResponse {
    private String month;
    private List<Insight> insights;
    private List<AnomalyDto> anomalies;

    @Getter
    @AllArgsConstructor
    public static class Insight {
        /** e.g. TOP_CATEGORY, SPIKE, SAVINGS_RATE, BUDGET_WARNING, INFO */
        private String type;
        /** info | warning | success */
        private String severity;
        private String title;
        private String message;
    }
}

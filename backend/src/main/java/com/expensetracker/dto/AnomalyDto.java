package com.expensetracker.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AnomalyDto {
    String category;
    String recentSpend;
    String baselineAverage;
    /** recent / baseline as a string like "3.2". Integer-arithmetic friendly. */
    String multiplier;
}

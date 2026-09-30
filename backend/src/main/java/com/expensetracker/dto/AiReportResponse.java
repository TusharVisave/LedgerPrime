package com.expensetracker.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiReportResponse {
    String month;
    String report;
}

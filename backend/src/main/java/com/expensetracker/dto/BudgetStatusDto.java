package com.expensetracker.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BudgetStatusDto {
    private String month;
    /** Decimal strings. */
    private String budget;
    private String spent;
    private String remaining;
    private boolean exceeded;
    private boolean nearLimit;
}

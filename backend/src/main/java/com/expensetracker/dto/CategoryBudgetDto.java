package com.expensetracker.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CategoryBudgetDto {
    String category;
    String budget;
    String spent;
    String remaining;
    boolean exceeded;
    boolean nearLimit;
    boolean rollover;
}

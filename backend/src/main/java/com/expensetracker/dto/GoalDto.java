package com.expensetracker.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GoalDto {
    Long id;
    String name;
    String targetAmount;
    long targetPaise;
    String deadline;
    String allocated;
    long allocatedPaise;
    String remaining;
    /** 0..100 progress percent (integer arithmetic, no float money). */
    int progressPercent;
}

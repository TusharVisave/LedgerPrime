package com.expensetracker.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class ReminderDto {
    private Long id;
    private String title;
    private String amount;
    private long amountPaise;
    private String category;
    private LocalDate dueDate;
    private boolean isRecurring;
    private String recurrence;
    private boolean emailNotify;
    private Instant lastTriggeredAt;
}

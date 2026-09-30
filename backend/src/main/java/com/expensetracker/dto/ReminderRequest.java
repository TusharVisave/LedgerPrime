package com.expensetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReminderRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title must be at most 120 characters")
    private String title;

    @Pattern(regexp = TransactionRequest.AMOUNT_PATTERN,
            message = "Amount must be a number with at most 2 decimal places")
    private String amount;

    @Size(max = 60, message = "Category must be at most 60 characters")
    private String category;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private Boolean isRecurring;

    @Pattern(regexp = "^(monthly)$", message = "Recurrence must be 'monthly'")
    private String recurrence;

    private Boolean emailNotify;
}

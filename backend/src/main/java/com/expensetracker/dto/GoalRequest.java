package com.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoalRequest {
    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must be at most 120 characters")
    private String name;

    /** Decimal string, e.g. "50000.00". Strict paise conversion. */
    @NotNull(message = "targetAmount is required")
    @DecimalMin(value = "0.01", message = "targetAmount must be positive")
    private String targetAmount;

    /** Deadline as yyyy-MM-dd. Optional. */
    private String deadline;
}

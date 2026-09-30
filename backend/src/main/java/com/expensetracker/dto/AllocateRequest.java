package com.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AllocateRequest {
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be positive")
    private String amount;

    @Size(max = 200, message = "note must be at most 200 characters")
    private String note;

    /** Allocation date as yyyy-MM-dd. Defaults to today. */
    private String date;
}

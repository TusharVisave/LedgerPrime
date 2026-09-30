package com.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetRequest {

    /** yyyy-MM; defaults to the current month when absent. */
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Month must be in yyyy-MM format")
    private String month;

    @NotBlank(message = "Amount is required")
    @Pattern(regexp = TransactionRequest.AMOUNT_PATTERN,
            message = "Amount must be a number with at most 2 decimal places")
    @DecimalMin(value = "0.00", message = "Amount must not be negative")
    private String amount;

    /**
     * Optional category for a per-category budget. Blank/absent means the
     * overall monthly budget.
     */
    private String category;

    /** When true, unused budget rolls into next month via the scheduled job. */
    private Boolean rollover;
}

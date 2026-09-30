package com.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class TransactionRequest {

    /** Decimal string like "1250.50". Never float — parsed to paise via BigDecimal. */
    public static final String AMOUNT_PATTERN = "^\\d+(\\.\\d{1,2})?$";

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "^(income|expense)$", message = "Type must be 'income' or 'expense'")
    private String type;

    @NotBlank(message = "Amount is required")
    @Pattern(regexp = AMOUNT_PATTERN, message = "Amount must be a number with at most 2 decimal places")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private String amount;

    @NotBlank(message = "Category is required")
    @Size(max = 60, message = "Category must be at most 60 characters")
    private String category;

    private List<@Size(max = 30, message = "Each tag must be at most 30 characters") String> tags;

    @Size(max = 500, message = "Note must be at most 500 characters")
    private String note;

    @NotNull(message = "Date is required")
    private LocalDate date;
}

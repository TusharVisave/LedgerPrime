package com.expensetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionParseRequest {
    @NotBlank(message = "text is required")
    @Size(max = 500, message = "text must be at most 500 characters")
    private String text;
}

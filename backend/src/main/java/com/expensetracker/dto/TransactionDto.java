package com.expensetracker.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TransactionDto {
    private Long id;
    private String type;
    /** Decimal string, e.g. "1250.50". */
    private String amount;
    /** Integer minor units (paise). */
    private long amountPaise;
    private String category;
    private List<String> tags;
    private String note;
    private LocalDate date;
    private boolean recurringGenerated;
    /** True when a receipt image is attached (GET /api/transactions/{id}/receipt). */
    private boolean hasReceipt;
    private Instant createdAt;
}

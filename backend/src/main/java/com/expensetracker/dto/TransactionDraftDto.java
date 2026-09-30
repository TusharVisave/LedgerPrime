package com.expensetracker.dto;

import lombok.Builder;
import lombok.Value;

/** Draft transaction produced by the rule-based natural-language parser. */
@Value
@Builder
public class TransactionDraftDto {
    /** "income" or "expense". */
    String type;
    /** Amount as decimal string, e.g. "250.00". Null when no amount was found. */
    String amount;
    /** Suggested category name. */
    String category;
    /** Date as yyyy-MM-dd. */
    String date;
    /** Original text, trimmed. */
    String note;
}

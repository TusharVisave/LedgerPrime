package com.expensetracker.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ImportResultDto {
    int importedCount;
    int failedCount;
    List<RowError> errors;

    @Value
    @Builder
    public static class RowError {
        int row;
        String message;
    }
}

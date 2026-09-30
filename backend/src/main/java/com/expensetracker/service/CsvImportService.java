package com.expensetracker.service;

import com.expensetracker.dto.ImportResultDto;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.entity.User;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV import: expects header columns date, description, amount, type.
 * Per-row validation; response reports imported count plus per-row errors.
 * Category is auto-suggested from the description via keyword matching.
 */
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
    };

    private final TransactionRepository transactionRepository;
    private final CategoryKeywordService categoryKeywords;

    @Transactional
    public ImportResultDto importCsv(User user, MultipartFile file) {
        List<ImportResultDto.RowError> errors = new ArrayList<>();
        int imported = 0;
        int rowNumber = 1; // header row

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                rowNumber++;
                try {
                    Transaction transaction = toTransaction(user, record);
                    transactionRepository.save(transaction);
                    imported++;
                } catch (IllegalArgumentException e) {
                    errors.add(ImportResultDto.RowError.builder()
                            .row(rowNumber)
                            .message(e.getMessage())
                            .build());
                }
            }
        } catch (IllegalArgumentException e) {
            throw com.expensetracker.exception.ApiException.badRequest(
                    "Invalid CSV file: " + e.getMessage());
        } catch (Exception e) {
            throw com.expensetracker.exception.ApiException.badRequest(
                    "Could not read CSV file: " + e.getMessage());
        }

        return ImportResultDto.builder()
                .importedCount(imported)
                .failedCount(errors.size())
                .errors(errors)
                .build();
    }

    private Transaction toTransaction(User user, CSVRecord record) {
        String dateRaw = get(record, "date");
        String description = get(record, "description");
        String amountRaw = get(record, "amount");
        String typeRaw = get(record, "type");

        if (dateRaw == null || dateRaw.isBlank()) {
            throw new IllegalArgumentException("date is required");
        }
        if (amountRaw == null || amountRaw.isBlank()) {
            throw new IllegalArgumentException("amount is required");
        }
        if (typeRaw == null || typeRaw.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }

        LocalDate date = parseDate(dateRaw);
        long amountPaise = parseAmount(amountRaw);
        TransactionType type = parseType(typeRaw);
        String category = categoryKeywords.suggest(description);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setType(type);
        transaction.setAmountPaise(amountPaise);
        transaction.setCategory(category);
        transaction.setNote(description == null ? "" : description);
        transaction.setDate(date);
        return transaction;
    }

    private String get(CSVRecord record, String column) {
        return record.isMapped(column) ? record.get(column) : null;
    }

    private LocalDate parseDate(String raw) {
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw.trim(), format);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        throw new IllegalArgumentException("date '" + raw + "' is not a supported format (yyyy-MM-dd or dd/MM/yyyy)");
    }

    private long parseAmount(String raw) {
        try {
            long paise = MoneyUtil.toPaise(raw.trim().replace(",", ""));
            if (paise <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
            return paise;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("amount '" + raw + "' is invalid: must be a number with at most 2 decimals");
        }
    }

    private TransactionType parseType(String raw) {
        String normalized = raw.trim().toLowerCase();
        if (normalized.equals("income")) {
            return TransactionType.INCOME;
        }
        if (normalized.equals("expense")) {
            return TransactionType.EXPENSE;
        }
        throw new IllegalArgumentException("type '" + raw + "' must be 'income' or 'expense'");
    }
}

package com.expensetracker.controller;

import com.expensetracker.dto.PageResponse;
import com.expensetracker.dto.ImportResultDto;
import com.expensetracker.dto.TransactionDraftDto;
import com.expensetracker.dto.TransactionDto;
import com.expensetracker.dto.TransactionParseRequest;
import com.expensetracker.dto.TransactionRequest;
import com.expensetracker.entity.User;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.CsvImportService;
import com.expensetracker.service.NaturalLanguageParser;
import com.expensetracker.service.TransactionService;
import com.expensetracker.service.TransactionService.TransactionWriteResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionService transactionService;
    private final NaturalLanguageParser naturalLanguageParser;
    private final CsvImportService csvImportService;
    private final UserRepository userRepository;

    @GetMapping
    public PageResponse<TransactionDto> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String minAmount,
            @RequestParam(required = false) String maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 25, sort = "date", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw com.expensetracker.exception.ApiException.badRequest("Page size must not exceed 100");
        }
        return transactionService.search(principal.getUserId(), type, category, keyword,
                minAmount, maxAmount, startDate, endDate, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionWriteResult create(@AuthenticationPrincipal UserPrincipal principal,
                                         @Valid @RequestBody TransactionRequest request) {
        return transactionService.create(principal.getUserId(), request);
    }

    @PutMapping("/{id}")
    public TransactionWriteResult update(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long id,
                                         @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(principal.getUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long id) {
        var budgetStatus = transactionService.delete(principal.getUserId(), id);
        return Map.of("message", "Transaction deleted", "budgetStatus", budgetStatus);
    }

    /**
     * Rule-based natural-language quick entry: turns free text like
     * "spent 500 on lunch today" into a draft the client confirms.
     */
    @PostMapping("/parse")
    public TransactionDraftDto parse(@AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody TransactionParseRequest request) {
        return naturalLanguageParser.parse(request.getText());
    }

    /**
     * CSV import: multipart "file" with columns date,description,amount,type
     * (+ optional category,tags,note). Returns per-row errors for bad rows.
     */
    @PostMapping("/import")
    public ImportResultDto importCsv(@AuthenticationPrincipal UserPrincipal principal,
                                     @RequestParam("file") MultipartFile file) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> com.expensetracker.exception.ApiException.unauthorized("User not found"));
        return csvImportService.importCsv(user, file);
    }
}

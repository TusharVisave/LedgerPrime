package com.expensetracker.service;

import com.expensetracker.dto.*;
import com.expensetracker.entity.*;
import com.expensetracker.repository.*;
import com.expensetracker.util.MoneyUtil;
import com.expensetracker.util.MonthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BudgetService budgetService;

    @Transactional(readOnly = true)
    public PageResponse<TransactionDto> search(Long userId,
                                               String type,
                                               String category,
                                               String keyword,
                                               String minAmount,
                                               String maxAmount,
                                               LocalDate startDate,
                                               LocalDate endDate,
                                               Pageable pageable) {
        TransactionType txType = type == null ? null : TransactionType.from(type);
        String likeKeyword = toLikePattern(keyword);
        Long minPaise = minAmount == null ? null : MoneyUtil.toPaise(minAmount);
        Long maxPaise = maxAmount == null ? null : MoneyUtil.toPaise(maxAmount);

        Page<Transaction> page = transactionRepository.search(
                userId, txType, blankToNull(category), likeKeyword,
                minPaise, maxPaise, startDate, endDate, pageable);

        List<TransactionDto> content = page.getContent().stream().map(this::toDto).toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    @Transactional
    public TransactionWriteResult create(Long userId, TransactionRequest request) {
        User user = getUser(userId);
        Transaction tx = new Transaction();
        tx.setUser(user);
        apply(request, tx);
        transactionRepository.save(tx);
        return new TransactionWriteResult(toDto(tx),
                budgetService.status(userId, MonthUtil.format(java.time.YearMonth.from(tx.getDate()))));
    }

    @Transactional
    public TransactionWriteResult update(Long userId, Long id, TransactionRequest request) {
        Transaction tx = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> com.expensetracker.exception.ApiException.notFound("Transaction not found"));
        apply(request, tx);
        transactionRepository.save(tx);
        return new TransactionWriteResult(toDto(tx),
                budgetService.status(userId, MonthUtil.format(java.time.YearMonth.from(tx.getDate()))));
    }

    @Transactional
    public BudgetStatusDto delete(Long userId, Long id) {
        Transaction tx = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> com.expensetracker.exception.ApiException.notFound("Transaction not found"));
        String month = MonthUtil.format(java.time.YearMonth.from(tx.getDate()));
        transactionRepository.delete(tx);
        return budgetService.status(userId, month);
    }

    private void apply(TransactionRequest request, Transaction tx) {
        tx.setType(TransactionType.from(request.getType()));
        tx.setAmountPaise(MoneyUtil.toPaise(request.getAmount()));
        tx.setCategory(request.getCategory().trim());
        tx.setTags(request.getTags() == null ? new java.util.ArrayList<>()
                : request.getTags().stream().map(String::trim).filter(s -> !s.isEmpty())
                        .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
        tx.setNote(request.getNote() == null ? "" : request.getNote().trim());
        tx.setDate(request.getDate());
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> com.expensetracker.exception.ApiException.unauthorized("User not found"));
    }

    public TransactionDto toDto(Transaction tx) {
        return TransactionDto.builder()
                .id(tx.getId())
                .type(tx.getType().jsonValue())
                .amount(MoneyUtil.toDecimalString(tx.getAmountPaise()))
                .amountPaise(tx.getAmountPaise())
                .category(tx.getCategory())
                .tags(List.copyOf(tx.getTags()))
                .note(tx.getNote())
                .date(tx.getDate())
                .recurringGenerated(tx.isRecurringGenerated())
                .hasReceipt(tx.getReceiptImagePath() != null)
                .createdAt(tx.getCreatedAt())
                .build();
    }

    /**
     * Escapes LIKE wildcards (%, _, \) in user input so a keyword can never
     * act as a wildcard pattern. Returns null when the keyword is blank.
     */
    static String toLikePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String escaped = keyword.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    public record TransactionWriteResult(TransactionDto transaction, BudgetStatusDto budgetStatus) {}
}

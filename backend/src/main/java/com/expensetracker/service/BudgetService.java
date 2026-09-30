package com.expensetracker.service;

import com.expensetracker.dto.BudgetRequest;
import com.expensetracker.dto.BudgetStatusDto;
import com.expensetracker.dto.CategoryBudgetDto;
import com.expensetracker.entity.Budget;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.entity.User;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.util.MoneyUtil;
import com.expensetracker.util.MonthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public BudgetStatusDto status(Long userId, String month) {
        String key = monthKey(month);
        LocalDate[] range = MonthUtil.monthRange(key);
        long budgetPaise = budgetRepository.findByUserIdAndMonthAndCategory(userId, key, "")
                .map(Budget::getAmountPaise).orElse(0L);
        long spentPaise = transactionRepository.sumByUserIdAndTypeAndDateBetween(
                userId, TransactionType.EXPENSE, range[0], range[1]);
        return toStatus(key, budgetPaise, spentPaise);
    }

    /** Per-category budget progress bars for the dashboard. */
    @Transactional(readOnly = true)
    public List<CategoryBudgetDto> categoryStatuses(Long userId, String month) {
        String key = monthKey(month);
        LocalDate[] range = MonthUtil.monthRange(key);
        return budgetRepository.findByUserIdAndMonth(userId, key).stream()
                .filter(b -> b.getCategory() != null && !b.getCategory().isBlank())
                .map(budget -> {
                    long spentPaise = transactionRepository.sumByUserIdAndTypeAndCategoryAndDateBetween(
                            userId, TransactionType.EXPENSE, budget.getCategory(), range[0], range[1]);
                    BudgetStatusDto status = toStatus(key, budget.getAmountPaise(), spentPaise);
                    return CategoryBudgetDto.builder()
                            .category(budget.getCategory())
                            .budget(status.getBudget())
                            .spent(status.getSpent())
                            .remaining(status.getRemaining())
                            .exceeded(status.isExceeded())
                            .nearLimit(status.isNearLimit())
                            .rollover(budget.isRollover())
                            .build();
                })
                .toList();
    }

    @Transactional
    public BudgetStatusDto setBudget(Long userId, BudgetRequest request) {
        String key = monthKey(request.getMonth());
        String category = request.getCategory() == null ? "" : request.getCategory().trim();
        long amountPaise = MoneyUtil.toPaise(request.getAmount());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> com.expensetracker.exception.ApiException.unauthorized("User not found"));

        Budget budget = budgetRepository.findByUserIdAndMonthAndCategory(userId, key, category)
                .orElseGet(Budget::new);
        budget.setUser(user);
        budget.setMonth(key);
        budget.setCategory(category);
        budget.setAmountPaise(amountPaise);
        if (request.getRollover() != null) {
            budget.setRollover(request.getRollover());
        }
        budgetRepository.save(budget);
        return status(userId, key);
    }

    /**
     * Carries unused budget into the next month for budgets with rollover on.
     * Idempotent per (source month, target month): only applies when the target
     * month has no budget row yet for that category, otherwise adds nothing.
     */
    @Transactional
    public void applyRollover(String fromMonth, String toMonth) {
        LocalDate[] range = MonthUtil.monthRange(fromMonth);
        for (Budget budget : budgetRepository.findByRolloverTrue()) {
            if (!budget.getMonth().equals(fromMonth)) {
                continue;
            }
            long spentPaise = budget.getCategory().isBlank()
                    ? transactionRepository.sumByUserIdAndTypeAndDateBetween(
                            budget.getUser().getId(), TransactionType.EXPENSE, range[0], range[1])
                    : transactionRepository.sumByUserIdAndTypeAndCategoryAndDateBetween(
                            budget.getUser().getId(), TransactionType.EXPENSE,
                            budget.getCategory(), range[0], range[1]);
            long unused = budget.getAmountPaise() - spentPaise;
            if (unused <= 0) {
                continue;
            }
            Long userId = budget.getUser().getId();
            String category = budget.getCategory() == null ? "" : budget.getCategory();
            budgetRepository.findByUserIdAndMonthAndCategory(userId, toMonth, category)
                    .ifPresentOrElse(
                            existing -> existing.setAmountPaise(existing.getAmountPaise() + unused),
                            () -> {
                                Budget next = new Budget();
                                next.setUser(budget.getUser());
                                next.setMonth(toMonth);
                                next.setCategory(category);
                                next.setAmountPaise(unused);
                                next.setRollover(true);
                                budgetRepository.save(next);
                            });
            budgetRepository.flush();
        }
    }

    private BudgetStatusDto toStatus(String month, long budgetPaise, long spentPaise) {
        long remainingPaise = Math.max(budgetPaise - spentPaise, 0);
        // Integer arithmetic: near limit means spent is 80-100% of budget.
        boolean nearLimit = budgetPaise > 0 && spentPaise * 5 >= budgetPaise * 4 && spentPaise <= budgetPaise;
        return BudgetStatusDto.builder()
                .month(month)
                .budget(MoneyUtil.toDecimalString(budgetPaise))
                .spent(MoneyUtil.toDecimalString(spentPaise))
                .remaining(MoneyUtil.toDecimalString(remainingPaise))
                .exceeded(budgetPaise > 0 && spentPaise > budgetPaise)
                .nearLimit(nearLimit)
                .build();
    }

    private String monthKey(String month) {
        String key = (month == null || month.isBlank()) ? MonthUtil.currentMonthKey() : month;
        if (!key.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw com.expensetracker.exception.ApiException.badRequest("Month must be in yyyy-MM format");
        }
        return key;
    }
}

package com.expensetracker.service;

import com.expensetracker.dto.DashboardDto;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.util.MoneyUtil;
import com.expensetracker.util.MonthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetService budgetService;
    private final TransactionService transactionService;

    @Transactional(readOnly = true)
    public DashboardDto dashboard(Long userId, String month) {
        String key = (month == null || month.isBlank()) ? MonthUtil.currentMonthKey() : month;
        LocalDate[] range = MonthUtil.monthRange(key);

        long income = transactionRepository.sumByUserIdAndTypeAndDateBetween(
                userId, TransactionType.INCOME, range[0], range[1]);
        long expense = transactionRepository.sumByUserIdAndTypeAndDateBetween(
                userId, TransactionType.EXPENSE, range[0], range[1]);

        Map<String, String> colors = new HashMap<>();
        categoryRepository.findByUserIdOrderByNameAsc(userId)
                .forEach(c -> colors.put(c.getName().toLowerCase(), c.getColor()));

        List<DashboardDto.CategorySlice> slices = transactionRepository
                .sumByCategory(userId, TransactionType.EXPENSE, range[0], range[1]).stream()
                .map(ct -> DashboardDto.CategorySlice.builder()
                        .name(ct.getName())
                        .value(MoneyUtil.toDecimalString(ct.getTotal()))
                        .color(colors.getOrDefault(ct.getName().toLowerCase(), "#1d4ed8"))
                        .build())
                .toList();

        List<DashboardDto.MonthPoint> trend = monthlyTrend(userId);

        List<DashboardDto.DayPoint> daily = transactionRepository
                .sumByDay(userId, TransactionType.EXPENSE, range[0], range[1]).stream()
                .map(d -> DashboardDto.DayPoint.builder()
                        .day(d.getDay())
                        .amount(MoneyUtil.toDecimalString(d.getTotal()))
                        .build())
                .toList();

        List<com.expensetracker.dto.TransactionDto> recent = transactionRepository
                .findTop7ByUserIdOrderByDateDescCreatedAtDesc(userId).stream()
                .map(transactionService::toDto)
                .toList();

        return DashboardDto.builder()
                .summary(DashboardDto.Summary.builder()
                        .month(key)
                        .income(MoneyUtil.toDecimalString(income))
                        .expense(MoneyUtil.toDecimalString(expense))
                        .balance(MoneyUtil.toDecimalString(income - expense))
                        .build())
                .budget(budgetService.status(userId, key))
                .categoryBudgets(budgetService.categoryStatuses(userId, key))
                .categorySpending(slices)
                .monthlyTrend(trend)
                .dailyTrend(daily)
                .recentTransactions(recent)
                .build();
    }

    /** Last 6 months including the current one. */
    private List<DashboardDto.MonthPoint> monthlyTrend(Long userId) {
        Map<String, long[]> byMonth = new HashMap<>();
        for (TransactionRepository.MonthlyTotal mt : transactionRepository.sumByMonth(userId)) {
            String key = String.format("%04d-%02d", mt.getY(), mt.getM());
            long[] sums = byMonth.computeIfAbsent(key, k -> new long[2]);
            if (mt.getType() == TransactionType.INCOME) {
                sums[0] = mt.getTotal();
            } else {
                sums[1] = mt.getTotal();
            }
        }
        List<DashboardDto.MonthPoint> trend = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            String key = MonthUtil.format(ym);
            long[] sums = byMonth.getOrDefault(key, new long[2]);
            trend.add(DashboardDto.MonthPoint.builder()
                    .month(key)
                    .income(MoneyUtil.toDecimalString(sums[0]))
                    .expense(MoneyUtil.toDecimalString(sums[1]))
                    .build());
        }
        return trend;
    }
}

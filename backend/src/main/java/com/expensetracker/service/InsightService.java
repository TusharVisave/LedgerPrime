package com.expensetracker.service;

import com.expensetracker.dto.InsightsResponse;
import com.expensetracker.dto.InsightsResponse.Insight;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.util.MoneyUtil;
import com.expensetracker.util.MonthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Rule-based spending insights — same spirit as the original insight controller. */
@Service
@RequiredArgsConstructor
public class InsightService {

    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;
    private final AnomalyService anomalyService;

    @Transactional(readOnly = true)
    public InsightsResponse insights(Long userId, String month) {
        String key = (month == null || month.isBlank()) ? MonthUtil.currentMonthKey() : month;
        LocalDate[] range = MonthUtil.monthRange(key);
        List<Insight> insights = new ArrayList<>();

        List<TransactionRepository.CategoryTotal> spending = transactionRepository.sumByCategory(
                userId, TransactionType.EXPENSE, range[0], range[1]);
        long total = spending.stream().mapToLong(TransactionRepository.CategoryTotal::getTotal).sum();
        long income = transactionRepository.sumByUserIdAndTypeAndDateBetween(
                userId, TransactionType.INCOME, range[0], range[1]);

        if (!spending.isEmpty() && total > 0) {
            var top = spending.get(0);
            long percent = Math.round(top.getTotal() * 100.0 / total);
            insights.add(new Insight("TOP_CATEGORY", "info", "Top spending category",
                    "Your highest spending is " + top.getName() + " at " + percent
                            + "% of this month's expenses (" + inr(top.getTotal()) + ")."));

            spending.stream()
                    .filter(c -> c.getName().equalsIgnoreCase("food"))
                    .filter(c -> c.getTotal() * 1.0 / total >= 0.3)
                    .findFirst()
                    .ifPresent(c -> insights.add(new Insight("CATEGORY_SHARE", "warning",
                            "Food spending is high",
                            "Food is " + Math.round(c.getTotal() * 100.0 / total)
                                    + "% of spending. Consider a smaller weekly meal budget.")));

            spending.stream()
                    .filter(c -> c.getName().equalsIgnoreCase("travel"))
                    .filter(c -> c.getTotal() * 1.0 / total >= 0.2)
                    .findFirst()
                    .ifPresent(c -> insights.add(new Insight("CATEGORY_SHARE", "info",
                            "Travel is a meaningful share",
                            "Travel is " + Math.round(c.getTotal() * 100.0 / total)
                                    + "% of spending. Review whether rides or fuel can be optimized.")));

            // Unusual spike vs the previous 3-month average, per category.
            YearMonth current = MonthUtil.parseMonth(key);
            for (var cat : spending) {
                long prevTotal = 0;
                for (int i = 1; i <= 3; i++) {
                    YearMonth ym = current.minusMonths(i);
                    LocalDate[] r = MonthUtil.monthRange(MonthUtil.format(ym));
                    prevTotal += transactionRepository.sumByCategory(
                                    userId, TransactionType.EXPENSE, r[0], r[1]).stream()
                            .filter(c -> c.getName().equalsIgnoreCase(cat.getName()))
                            .mapToLong(TransactionRepository.CategoryTotal::getTotal)
                            .sum();
                }
                long prevAvg = prevTotal / 3;
                if (prevAvg > 0 && cat.getTotal() > prevAvg * 1.5) {
                    insights.add(new Insight("SPIKE", "warning",
                            "Unusual spike in " + cat.getName(),
                            cat.getName() + " spending (" + inr(cat.getTotal())
                                    + ") is well above its 3-month average of "
                                    + inr(prevAvg) + "."));
                }
            }
        }

        // Savings rate.
        if (income > 0) {
            double rate = (income - total) * 100.0 / income;
            if (rate >= 20) {
                insights.add(new Insight("SAVINGS_RATE", "success", "Healthy savings rate",
                        "You're saving " + Math.round(rate) + "% of income this month. Well done."));
            } else if (rate < 0) {
                insights.add(new Insight("SAVINGS_RATE", "warning", "Spending exceeds income",
                        "Expenses exceed income by " + inr(total - income) + " this month."));
            } else {
                insights.add(new Insight("SAVINGS_RATE", "info", "Savings rate",
                        "You're saving " + Math.round(rate) + "% of income this month."));
            }
        }

        // Budget warnings.
        var budget = budgetService.status(userId, key);
        if (budget.isExceeded()) {
            insights.add(new Insight("BUDGET_WARNING", "warning", "Budget exceeded",
                    "You've spent " + inr(MoneyUtil.toPaise(budget.getSpent()))
                            + " against a " + inr(MoneyUtil.toPaise(budget.getBudget())) + " budget."));
        } else if (budget.isNearLimit()) {
            insights.add(new Insight("BUDGET_WARNING", "warning", "Budget nearly exhausted",
                    "You've used over 80% of this month's budget ("
                            + inr(MoneyUtil.toPaise(budget.getRemaining())) + " left)."));
        }

        if (insights.isEmpty()) {
            insights.add(new Insight("INFO", "info", "Balanced month",
                    "Spending looks balanced this month. Keep tracking categories to improve future recommendations."));
        }
        return new InsightsResponse(key, insights, anomalyService.detect(userId));
    }

    private static String inr(long paise) {
        return MoneyUtil.toInrString(paise);
    }
}

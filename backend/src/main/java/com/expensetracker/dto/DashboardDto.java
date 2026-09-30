package com.expensetracker.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardDto {

    @Getter
    @Builder
    public static class Summary {
        private String month;
        private String income;
        private String expense;
        private String balance;
    }

    @Getter
    @Builder
    public static class CategorySlice {
        private String name;
        private String value;
        private String color;
    }

    @Getter
    @Builder
    public static class MonthPoint {
        private String month;
        private String income;
        private String expense;
    }

    @Getter
    @Builder
    public static class DayPoint {
        private int day;
        private String amount;
    }

    private Summary summary;
    private BudgetStatusDto budget;
    private List<CategorySlice> categorySpending;
    private List<CategoryBudgetDto> categoryBudgets;
    private List<MonthPoint> monthlyTrend;
    private List<DayPoint> dailyTrend;
    private List<TransactionDto> recentTransactions;
}

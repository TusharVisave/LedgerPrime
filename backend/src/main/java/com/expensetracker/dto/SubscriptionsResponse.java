package com.expensetracker.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SubscriptionsResponse {
    List<SubscriptionDto> subscriptions;
    String totalMonthlyCost;
    long totalMonthlyCostPaise;
    int count;

    @Value
    @Builder
    public static class SubscriptionDto {
        Long id;
        String title;
        String amount;
        String category;
        String dueDate;
        String recurrence;
        String monthlyCost;
    }
}

package com.expensetracker.service;

import com.expensetracker.dto.SubscriptionsResponse;
import com.expensetracker.entity.Reminder;
import com.expensetracker.repository.ReminderRepository;
import com.expensetracker.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Subscriptions are derived from active recurring reminders. Currently only
 * monthly recurrence is modeled, so monthly cost equals the reminder amount.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final ReminderRepository reminderRepository;

    @Transactional(readOnly = true)
    public SubscriptionsResponse list(Long userId) {
        List<Reminder> recurring = reminderRepository.findByUserIdOrderByDueDateAsc(userId).stream()
                .filter(Reminder::isRecurring)
                .toList();

        List<SubscriptionsResponse.SubscriptionDto> subscriptions = recurring.stream()
                .map(r -> SubscriptionsResponse.SubscriptionDto.builder()
                        .id(r.getId())
                        .title(r.getTitle())
                        .amount(MoneyUtil.toDecimalString(r.getAmountPaise()))
                        .category(r.getCategory())
                        .dueDate(r.getDueDate() == null ? null : r.getDueDate().toString())
                        .recurrence(r.getRecurrence() == null ? "monthly" : r.getRecurrence())
                        .monthlyCost(MoneyUtil.toDecimalString(r.getAmountPaise()))
                        .build())
                .toList();

        long totalPaise = recurring.stream().mapToLong(Reminder::getAmountPaise).sum();
        return SubscriptionsResponse.builder()
                .subscriptions(subscriptions)
                .totalMonthlyCost(MoneyUtil.toDecimalString(totalPaise))
                .totalMonthlyCostPaise(totalPaise)
                .count(subscriptions.size())
                .build();
    }
}

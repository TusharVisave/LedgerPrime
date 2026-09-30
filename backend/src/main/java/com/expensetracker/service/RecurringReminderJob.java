package com.expensetracker.service;

import com.expensetracker.entity.Reminder;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.repository.ReminderRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Daily 8 AM job: (a) generates recurring transactions from due recurring
 * reminders, (b) emails users about reminders due within 3 days.
 */
@Component
@RequiredArgsConstructor
public class RecurringReminderJob {

    private static final Logger log = LoggerFactory.getLogger(RecurringReminderJob.class);

    private final ReminderRepository reminderRepository;
    private final TransactionRepository transactionRepository;
    private final MailNotifier mailNotifier;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void run() {
        processRecurring(LocalDate.now(), Instant.now());
        notifyUpcoming(LocalDate.now());
    }

    void processRecurring(LocalDate today, Instant now) {
        for (Reminder reminder : reminderRepository.findByIsRecurringTrue()) {
            boolean sameDayOfMonth = reminder.getDueDate().getDayOfMonth() == today.getDayOfMonth();
            boolean alreadyTriggeredThisMonth = reminder.getLastTriggeredAt() != null
                    && YearMonth.from(reminder.getLastTriggeredAt()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                            .equals(YearMonth.from(today));
            if (!sameDayOfMonth || alreadyTriggeredThisMonth) {
                continue;
            }
            Transaction tx = new Transaction();
            tx.setUser(reminder.getUser());
            tx.setType(TransactionType.EXPENSE);
            tx.setAmountPaise(reminder.getAmountPaise());
            tx.setCategory(reminder.getCategory());
            tx.setDate(today);
            tx.setNote("Auto-added recurring expense: " + reminder.getTitle());
            tx.setRecurringGenerated(true);
            tx.setRecurringSourceId(reminder.getId());
            transactionRepository.save(tx);

            reminder.setLastTriggeredAt(now);
            reminderRepository.save(reminder);
            log.info("Generated recurring transaction for reminder {}", reminder.getId());
        }
    }

    void notifyUpcoming(LocalDate today) {
        // Upcoming email notifications across every user. Deduped via
        // lastNotifiedAt: a reminder is emailed at most once per day.
        reminderRepository.findAll().stream()
                .filter(Reminder::isEmailNotify)
                .filter(r -> {
                    LocalDate due = r.getDueDate();
                    return !due.isBefore(today) && !due.isAfter(today.plusDays(3));
                })
                .filter(r -> r.getLastNotifiedAt() == null
                        || r.getLastNotifiedAt().isBefore(today.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()))
                .forEach(r -> {
                    try {
                        mailNotifier.sendReminderEmail(
                                r.getUser().getEmail(), r.getTitle(), r.getDueDate(),
                                MoneyUtil.toDecimalString(r.getAmountPaise()));
                        r.setLastNotifiedAt(java.time.Instant.now());
                        reminderRepository.save(r);
                    } catch (Exception e) {
                        log.warn("Upcoming-reminder email failed for reminder {}", r.getId(), e);
                    }
                });
    }
}

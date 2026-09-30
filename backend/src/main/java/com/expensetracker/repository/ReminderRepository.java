package com.expensetracker.repository;

import com.expensetracker.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUserIdOrderByDueDateAsc(Long userId);

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    List<Reminder> findByUserIdAndDueDateBetweenOrderByDueDateAsc(Long userId, LocalDate from, LocalDate to);

    List<Reminder> findByIsRecurringTrue();
}

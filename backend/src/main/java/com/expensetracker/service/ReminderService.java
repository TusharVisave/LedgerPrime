package com.expensetracker.service;

import com.expensetracker.dto.ReminderDto;
import com.expensetracker.dto.ReminderRequest;
import com.expensetracker.entity.Reminder;
import com.expensetracker.entity.User;
import com.expensetracker.exception.ApiException;
import com.expensetracker.repository.ReminderRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final MailNotifier mailNotifier;

    @Transactional(readOnly = true)
    public List<ReminderDto> list(Long userId) {
        return reminderRepository.findByUserIdOrderByDueDateAsc(userId).stream()
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ReminderDto> upcoming(Long userId, int days) {
        LocalDate today = LocalDate.now();
        return reminderRepository
                .findByUserIdAndDueDateBetweenOrderByDueDateAsc(userId, today, today.plusDays(days))
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public ReminderDto create(Long userId, ReminderRequest request) {
        User user = getUser(userId);
        Reminder reminder = new Reminder();
        reminder.setUser(user);
        apply(request, reminder);
        reminderRepository.save(reminder);

        if (reminder.isEmailNotify()) {
            try {
                mailNotifier.sendReminderEmail(user.getEmail(), reminder.getTitle(),
                        reminder.getDueDate(), MoneyUtil.toDecimalString(reminder.getAmountPaise()));
            } catch (Exception e) {
                log.warn("Reminder email failed (continuing)", e);
            }
        }
        return toDto(reminder);
    }

    @Transactional
    public ReminderDto update(Long userId, Long id, ReminderRequest request) {
        Reminder reminder = reminderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Reminder not found"));
        apply(request, reminder);
        return toDto(reminderRepository.save(reminder));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Reminder reminder = reminderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Reminder not found"));
        reminderRepository.delete(reminder);
    }

    private void apply(ReminderRequest request, Reminder reminder) {
        reminder.setTitle(request.getTitle().trim());
        reminder.setAmountPaise(request.getAmount() == null ? 0 : MoneyUtil.toPaise(request.getAmount()));
        reminder.setCategory(request.getCategory() == null || request.getCategory().isBlank()
                ? "Bills" : request.getCategory().trim());
        reminder.setDueDate(request.getDueDate());
        reminder.setRecurring(Boolean.TRUE.equals(request.getIsRecurring()));
        reminder.setRecurrence(request.getRecurrence() == null ? "monthly" : request.getRecurrence());
        reminder.setEmailNotify(Boolean.TRUE.equals(request.getEmailNotify()));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
    }

    public ReminderDto toDto(Reminder reminder) {
        return ReminderDto.builder()
                .id(reminder.getId())
                .title(reminder.getTitle())
                .amount(MoneyUtil.toDecimalString(reminder.getAmountPaise()))
                .amountPaise(reminder.getAmountPaise())
                .category(reminder.getCategory())
                .dueDate(reminder.getDueDate())
                .isRecurring(reminder.isRecurring())
                .recurrence(reminder.getRecurrence())
                .emailNotify(reminder.isEmailNotify())
                .lastTriggeredAt(reminder.getLastTriggeredAt())
                .build();
    }
}

package com.expensetracker.service;

import java.time.LocalDate;

/** Sends reminder emails. Disabled by default via app.mail.enabled. */
public interface MailNotifier {
    void sendReminderEmail(String to, String title, LocalDate dueDate, String amountDecimal);
}

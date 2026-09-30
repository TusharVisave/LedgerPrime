package com.expensetracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** No-op notifier used when app.mail.enabled=false (the default). */
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMailNotifier implements MailNotifier {

    private static final Logger log = LoggerFactory.getLogger(NoOpMailNotifier.class);

    @Override
    public void sendReminderEmail(String to, String title, LocalDate dueDate, String amountDecimal) {
        log.debug("Mail disabled — skipping reminder email to {} for '{}'", to, title);
    }
}

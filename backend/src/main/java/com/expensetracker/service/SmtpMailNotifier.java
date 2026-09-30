package com.expensetracker.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SmtpMailNotifier implements MailNotifier {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailNotifier.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@expensetracker.local}")
    private String from;

    @Override
    public void sendReminderEmail(String to, String title, LocalDate dueDate, String amountDecimal) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Reminder: " + title);
        message.setText("Reminder for " + title + " due on "
                + dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + ". Amount: INR " + amountDecimal + ".");
        mailSender.send(message);
        log.info("Sent reminder email to {} for '{}'", to, title);
    }
}

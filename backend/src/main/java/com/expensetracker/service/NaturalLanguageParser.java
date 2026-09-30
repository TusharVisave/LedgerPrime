package com.expensetracker.service;

import com.expensetracker.dto.TransactionDraftDto;
import com.expensetracker.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based natural-language transaction parser. No AI involved:
 * amount (₹/Rs/plain numbers), category via keyword map, date words
 * (today/yesterday/weekday names), and type (spent/paid -> expense,
 * received/got -> income).
 */
@Service
@RequiredArgsConstructor
public class NaturalLanguageParser {

    private static final Pattern AMOUNT = Pattern.compile(
            "(?:₹|rs\\.?|inr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INCOME_WORDS = Pattern.compile(
            "(?i)\\b(received|receive|got|get|earned|earn|income|salary|refund|credited)\\b");
    private static final Pattern EXPENSE_WORDS = Pattern.compile(
            "(?i)\\b(spent|spend|paid|pay|bought|buy|purchase|purchased|ordered|order)\\b");
    private static final Pattern YESTERDAY = Pattern.compile("(?i)\\byesterday\\b");
    private static final Pattern TODAY = Pattern.compile("(?i)\\btoday\\b");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final CategoryKeywordService categoryKeywords;

    public TransactionDraftDto parse(String text) {
        String trimmed = text == null ? "" : text.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        String amount = extractAmount(trimmed);
        String type = "expense";
        if (INCOME_WORDS.matcher(lower).find() && !EXPENSE_WORDS.matcher(lower).find()) {
            type = "income";
        }
        String category = categoryKeywords.suggest(trimmed);
        String date = extractDate(lower);

        return TransactionDraftDto.builder()
                .type(type)
                .amount(amount)
                .category(category)
                .date(date)
                .note(trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed)
                .build();
    }

    private String extractAmount(String text) {
        Matcher matcher = AMOUNT.matcher(text);
        while (matcher.find()) {
            String raw = matcher.group(1).replace(",", "");
            try {
                BigDecimal value = new BigDecimal(raw);
                if (value.compareTo(BigDecimal.ZERO) > 0) {
                    long paise = MoneyUtil.toPaise(value.toPlainString());
                    return MoneyUtil.toDecimalString(paise);
                }
            } catch (IllegalArgumentException ignored) {
                // not a valid amount, try the next number
            }
        }
        return null;
    }

    private String extractDate(String lower) {
        LocalDate today = LocalDate.now();
        if (YESTERDAY.matcher(lower).find()) {
            return today.minusDays(1).format(DATE_FMT);
        }
        for (DayOfWeek day : DayOfWeek.values()) {
            String name = day.name().toLowerCase(Locale.ROOT);
            if (Pattern.compile("\\b" + name + "\\b").matcher(lower).find()) {
                LocalDate date = today;
                while (date.getDayOfWeek() != day) {
                    date = date.minusDays(1);
                }
                return date.format(DATE_FMT);
            }
        }
        if (TODAY.matcher(lower).find()) {
            return today.format(DATE_FMT);
        }
        return today.format(DATE_FMT);
    }
}

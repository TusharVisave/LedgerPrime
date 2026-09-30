package com.expensetracker.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public final class MonthUtil {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private MonthUtil() {
    }

    /** Current month as "yyyy-MM". */
    public static String currentMonthKey() {
        return YearMonth.now().format(MONTH_FORMAT);
    }

    public static YearMonth parseMonth(String monthKey) {
        return YearMonth.parse(monthKey, MONTH_FORMAT);
    }

    /** Inclusive start/end dates of a "yyyy-MM" month. */
    public static LocalDate[] monthRange(String monthKey) {
        YearMonth ym = parseMonth(monthKey);
        return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
    }

    public static String format(YearMonth ym) {
        return ym.format(MONTH_FORMAT);
    }
}

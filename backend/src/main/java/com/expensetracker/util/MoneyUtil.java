package com.expensetracker.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money conversions. All monetary values are stored as {@code long} minor units
 * (paise) in the database. The API accepts/returns decimal strings such as
 * "1250.50". No float/double is ever used for money math.
 */
public final class MoneyUtil {

    private MoneyUtil() {
    }

    /** "1250.50" -> 125050. Throws IllegalArgumentException on invalid input. */
    public static long toPaise(String decimal) {
        if (decimal == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        try {
            BigDecimal value = new BigDecimal(decimal.trim());
            return value.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount: " + decimal);
        }
    }

    /** 125050 -> "1250.50". */
    public static String toDecimalString(long paise) {
        return BigDecimal.valueOf(paise, 2).toPlainString();
    }

    /** 125050 -> "₹1,250.50" using en-IN grouping. No floating point. */
    public static String toInrString(long paise) {
        BigDecimal value = BigDecimal.valueOf(paise, 2);
        java.text.NumberFormat fmt = java.text.NumberFormat.getCurrencyInstance(
                new java.util.Locale("en", "IN"));
        fmt.setMaximumFractionDigits(2);
        fmt.setMinimumFractionDigits(2);
        return fmt.format(value);
    }
}

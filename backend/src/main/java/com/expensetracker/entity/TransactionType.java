package com.expensetracker.entity;

/** Transaction direction. Persisted as the lowercase name via attribute converter. */
public enum TransactionType {
    INCOME,
    EXPENSE;

    public static TransactionType from(String value) {
        if (value == null) {
            return null;
        }
        return TransactionType.valueOf(value.trim().toUpperCase());
    }

    public String jsonValue() {
        return name().toLowerCase();
    }
}

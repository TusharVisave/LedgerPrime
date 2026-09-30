package com.expensetracker.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Rule-based category suggestion shared by the natural-language parser,
 * CSV import and receipt scanning. Keyword map only — no AI involved.
 */
@Service
public class CategoryKeywordService {

    private static final Map<String, Pattern> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put("Food", Pattern.compile(
                "(?i)\\b(food|lunch|dinner|breakfast|brunch|restaurant|cafe|coffee|pizza|burger|" +
                "grocery|groceries|swiggy|zomato|blinkit|dominos|kfc|mcdonald|snack|tea|chai|bakery|hotel)\\b"));
        KEYWORDS.put("Travel", Pattern.compile(
                "(?i)\\b(petrol|fuel|diesel|cng|cab|taxi|uber|ola|bus|train|flight|travel|metro|" +
                "parking|toll|rickshaw|auto)\\b"));
        KEYWORDS.put("Rent", Pattern.compile(
                "(?i)\\b(rent|landlord|lease|deposit)\\b"));
        KEYWORDS.put("Bills", Pattern.compile(
                "(?i)\\b(bill|electricity|water|gas|lpg|internet|wifi|broadband|mobile|recharge|" +
                "dth|emi|loan|insurance|maintenance)\\b"));
        KEYWORDS.put("Shopping", Pattern.compile(
                "(?i)\\b(shopping|clothes|clothing|shoes|shirt|amazon|flipkart|myntra|mall|store|" +
                "purchase|electronics|gadget)\\b"));
        KEYWORDS.put("Health", Pattern.compile(
                "(?i)\\b(doctor|hospital|clinic|medicine|pharmacy|medical|gym|fitness|health)\\b"));
        KEYWORDS.put("Entertainment", Pattern.compile(
                "(?i)\\b(movie|cinema|netflix|hotstar|prime|game|gaming|concert|party|entertainment|subscription)\\b"));
        KEYWORDS.put("Salary", Pattern.compile(
                "(?i)\\b(salary|payroll|stipend)\\b"));
        KEYWORDS.put("Freelance", Pattern.compile(
                "(?i)\\b(freelance|contract|gig|consulting|client payment)\\b"));
    }

    /** Returns the best-guess category for free text, or "Other" when nothing matches. */
    public String suggest(String text) {
        if (text == null || text.isBlank()) {
            return "Other";
        }
        for (Map.Entry<String, Pattern> entry : KEYWORDS.entrySet()) {
            if (entry.getValue().matcher(text).find()) {
                return entry.getKey();
            }
        }
        return "Other";
    }
}

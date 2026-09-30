package com.expensetracker.service;

import com.expensetracker.dto.AnomalyDto;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.TransactionRepository.CategoryTotal;
import com.expensetracker.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based anomaly detection: compares last-7-days spend per category
 * against that category's 8-week average (the 8 weeks before the recent
 * window). A category is flagged when recent spend is >= 2.5x the baseline
 * and the baseline is positive. All arithmetic stays in integer paise;
 * the multiplier is formatted with BigDecimal, never float/double.
 */
@Service
@RequiredArgsConstructor
public class AnomalyService {

    /** Spike threshold: recent >= 2.5x baseline. */
    private static final BigDecimal THRESHOLD = new BigDecimal("2.5");
    /** Minimum baseline to avoid flagging noise: ₹50 in paise. */
    private static final long MIN_BASELINE_PAISE = 5_000L;

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<AnomalyDto> detect(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate recentStart = today.minusDays(6);
        LocalDate baselineStart = recentStart.minusWeeks(8);

        List<AnomalyDto> anomalies = new ArrayList<>();
        List<CategoryTotal> recent = transactionRepository.sumByCategory(
                userId, TransactionType.EXPENSE, recentStart, today);

        for (CategoryTotal row : recent) {
            long recentPaise = row.getTotal() == null ? 0 : row.getTotal();
            long baselinePaise = transactionRepository.sumByUserIdAndTypeAndCategoryAndDateBetween(
                    userId, TransactionType.EXPENSE, row.getName(),
                    baselineStart, recentStart.minusDays(1));
            long baselineAverage = baselinePaise / 8;
            if (baselineAverage < MIN_BASELINE_PAISE || recentPaise <= 0) {
                continue;
            }
            BigDecimal multiplier = new BigDecimal(recentPaise)
                    .divide(new BigDecimal(baselineAverage), 2, RoundingMode.HALF_UP);
            if (multiplier.compareTo(THRESHOLD) >= 0) {
                anomalies.add(AnomalyDto.builder()
                        .category(row.getName())
                        .recentSpend(MoneyUtil.toDecimalString(recentPaise))
                        .baselineAverage(MoneyUtil.toDecimalString(baselineAverage))
                        .multiplier(multiplier.toPlainString() + "x")
                        .build());
            }
        }
        return anomalies;
    }
}

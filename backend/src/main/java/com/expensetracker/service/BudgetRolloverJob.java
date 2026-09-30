package com.expensetracker.service;

import com.expensetracker.util.MonthUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

/**
 * Carries unused budget into the next month for budgets with rollover enabled.
 * Runs at 00:05 on the first day of each month.
 */
@Service
@RequiredArgsConstructor
public class BudgetRolloverJob {

    private static final Logger log = LoggerFactory.getLogger(BudgetRolloverJob.class);

    private final BudgetService budgetService;

    @Scheduled(cron = "0 5 0 1 * *")
    public void rollover() {
        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);
        String fromMonth = MonthUtil.format(previous);
        String toMonth = MonthUtil.format(current);
        log.info("Running budget rollover from {} to {}", fromMonth, toMonth);
        budgetService.applyRollover(fromMonth, toMonth);
    }
}

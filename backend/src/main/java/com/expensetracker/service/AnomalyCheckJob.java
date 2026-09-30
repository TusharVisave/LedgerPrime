package com.expensetracker.service;

import com.expensetracker.dto.AnomalyDto;
import com.expensetracker.entity.User;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduled anomaly scan: every Monday at 09:00, checks every user for
 * category spending spikes (last 7 days vs 8-week average) and logs them.
 * Users see the same check on demand via GET /api/insights/anomalies and
 * inside the insights response.
 */
@Service
@RequiredArgsConstructor
public class AnomalyCheckJob {

    private static final Logger log = LoggerFactory.getLogger(AnomalyCheckJob.class);

    private final AnomalyService anomalyService;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 9 * * MON")
    public void checkAllUsers() {
        List<User> users = userRepository.findAll();
        log.info("Running anomaly check for {} users", users.size());
        for (User user : users) {
            try {
                List<AnomalyDto> anomalies = anomalyService.detect(user.getId());
                for (AnomalyDto anomaly : anomalies) {
                    log.warn("Spending anomaly for user {}: {} at {} of 8-week average {}",
                            user.getId(), anomaly.getCategory(),
                            anomaly.getMultiplier(), anomaly.getBaselineAverage());
                }
            } catch (Exception e) {
                log.warn("Anomaly check failed for user {}: {}", user.getId(), e.toString());
            }
        }
    }
}

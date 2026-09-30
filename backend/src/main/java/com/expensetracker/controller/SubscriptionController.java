package com.expensetracker.controller;

import com.expensetracker.dto.SubscriptionsResponse;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Subscriptions derived from recurring reminders (treated as monthly).
 */
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public SubscriptionsResponse list(@AuthenticationPrincipal UserPrincipal principal) {
        return subscriptionService.list(principal.getUserId());
    }
}

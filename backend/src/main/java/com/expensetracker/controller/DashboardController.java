package com.expensetracker.controller;

import com.expensetracker.dto.DashboardDto;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardDto get(@AuthenticationPrincipal UserPrincipal principal,
                            @RequestParam(required = false) String month) {
        return dashboardService.dashboard(principal.getUserId(), month);
    }
}

package com.expensetracker.controller;

import com.expensetracker.dto.BudgetRequest;
import com.expensetracker.dto.BudgetStatusDto;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public BudgetStatusDto get(@AuthenticationPrincipal UserPrincipal principal,
                               @RequestParam(required = false) String month) {
        return budgetService.status(principal.getUserId(), month);
    }

    @GetMapping("/categories")
    public java.util.List<com.expensetracker.dto.CategoryBudgetDto> categories(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String month) {
        return budgetService.categoryStatuses(principal.getUserId(), month);
    }

    @PutMapping
    public BudgetStatusDto set(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody BudgetRequest request) {
        return budgetService.setBudget(principal.getUserId(), request);
    }
}

package com.expensetracker.controller;

import com.expensetracker.dto.ReminderDto;
import com.expensetracker.dto.ReminderRequest;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.ReminderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @GetMapping
    public List<ReminderDto> list(@AuthenticationPrincipal UserPrincipal principal) {
        return reminderService.list(principal.getUserId());
    }

    @GetMapping("/upcoming")
    public List<ReminderDto> upcoming(@AuthenticationPrincipal UserPrincipal principal,
                                     @RequestParam(defaultValue = "7")
                                     @Min(1) @Max(90) int days) {
        return reminderService.upcoming(principal.getUserId(), days);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderDto create(@AuthenticationPrincipal UserPrincipal principal,
                              @Valid @RequestBody ReminderRequest request) {
        return reminderService.create(principal.getUserId(), request);
    }

    @PutMapping("/{id}")
    public ReminderDto update(@AuthenticationPrincipal UserPrincipal principal,
                              @PathVariable Long id,
                              @Valid @RequestBody ReminderRequest request) {
        return reminderService.update(principal.getUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long id) {
        reminderService.delete(principal.getUserId(), id);
        return Map.of("message", "Reminder deleted");
    }
}

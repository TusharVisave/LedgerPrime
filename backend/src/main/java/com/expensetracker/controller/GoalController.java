package com.expensetracker.controller;

import com.expensetracker.dto.AllocateRequest;
import com.expensetracker.dto.GoalDto;
import com.expensetracker.dto.GoalRequest;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public List<GoalDto> list(@AuthenticationPrincipal UserPrincipal principal) {
        return goalService.list(principal.getUserId());
    }

    @GetMapping("/{id}")
    public GoalDto get(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable Long id) {
        return goalService.get(principal.getUserId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalDto create(@AuthenticationPrincipal UserPrincipal principal,
                          @Valid @RequestBody GoalRequest request) {
        return goalService.create(principal.getUserId(), request);
    }

    @PutMapping("/{id}")
    public GoalDto update(@AuthenticationPrincipal UserPrincipal principal,
                          @PathVariable Long id,
                          @Valid @RequestBody GoalRequest request) {
        return goalService.update(principal.getUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable Long id) {
        goalService.delete(principal.getUserId(), id);
    }

    @PostMapping("/{id}/allocate")
    public GoalDto allocate(@AuthenticationPrincipal UserPrincipal principal,
                            @PathVariable Long id,
                            @Valid @RequestBody AllocateRequest request) {
        return goalService.allocate(principal.getUserId(), id, request);
    }
}

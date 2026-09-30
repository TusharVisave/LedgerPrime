package com.expensetracker.service;

import com.expensetracker.dto.AllocateRequest;
import com.expensetracker.dto.GoalDto;
import com.expensetracker.dto.GoalRequest;
import com.expensetracker.entity.Goal;
import com.expensetracker.entity.GoalAllocation;
import com.expensetracker.entity.User;
import com.expensetracker.exception.ApiException;
import com.expensetracker.repository.GoalAllocationRepository;
import com.expensetracker.repository.GoalRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalAllocationRepository allocationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GoalDto> list(Long userId) {
        return goalRepository.findByUserIdOrderByDeadlineAsc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public GoalDto get(Long userId, Long goalId) {
        return toDto(requireGoal(userId, goalId));
    }

    @Transactional
    public GoalDto create(Long userId, GoalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
        long targetPaise = MoneyUtil.toPaise(request.getTargetAmount());
        if (targetPaise <= 0) {
            throw ApiException.badRequest("Target amount must be positive");
        }
        LocalDate deadline = parseDate(request.getDeadline(), "deadline");
        Goal goal = new Goal(user, request.getName().trim(), targetPaise, deadline);
        return toDto(goalRepository.save(goal));
    }

    @Transactional
    public GoalDto update(Long userId, Long goalId, GoalRequest request) {
        Goal goal = requireGoal(userId, goalId);
        long targetPaise = MoneyUtil.toPaise(request.getTargetAmount());
        if (targetPaise <= 0) {
            throw ApiException.badRequest("Target amount must be positive");
        }
        goal.setName(request.getName().trim());
        goal.setTargetPaise(targetPaise);
        goal.setDeadline(parseDate(request.getDeadline(), "deadline"));
        return toDto(goalRepository.save(goal));
    }

    @Transactional
    public void delete(Long userId, Long goalId) {
        goalRepository.delete(requireGoal(userId, goalId));
    }

    @Transactional
    public GoalDto allocate(Long userId, Long goalId, AllocateRequest request) {
        Goal goal = requireGoal(userId, goalId);
        long amountPaise = MoneyUtil.toPaise(request.getAmount());
        if (amountPaise <= 0) {
            throw ApiException.badRequest("Allocation amount must be positive");
        }
        LocalDate date = request.getDate() == null || request.getDate().isBlank()
                ? LocalDate.now()
                : parseDate(request.getDate(), "date");
        GoalAllocation allocation = new GoalAllocation(goal, amountPaise,
                request.getNote() == null ? "" : request.getNote().trim(), date);
        goal.getAllocations().add(allocation);
        allocationRepository.save(allocation);
        return toDto(goal);
    }

    private Goal requireGoal(Long userId, Long goalId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> ApiException.notFound("Goal not found"));
    }

    private LocalDate parseDate(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest(field + " must be in yyyy-MM-dd format");
        }
    }

    private GoalDto toDto(Goal goal) {
        long allocated = goal.getAllocatedPaise();
        long target = goal.getTargetPaise();
        int progress = target > 0 ? (int) Math.min(allocated * 100L / target, 100L) : 0;
        return GoalDto.builder()
                .id(goal.getId())
                .name(goal.getName())
                .targetAmount(MoneyUtil.toDecimalString(target))
                .targetPaise(target)
                .deadline(goal.getDeadline() == null ? null : goal.getDeadline().toString())
                .allocated(MoneyUtil.toDecimalString(allocated))
                .allocatedPaise(allocated)
                .remaining(MoneyUtil.toDecimalString(Math.max(target - allocated, 0)))
                .progressPercent(progress)
                .build();
    }
}

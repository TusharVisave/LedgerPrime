package com.expensetracker.repository;

import com.expensetracker.entity.GoalAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalAllocationRepository extends JpaRepository<GoalAllocation, Long> {
    List<GoalAllocation> findByGoalIdOrderByDateAsc(Long goalId);
}

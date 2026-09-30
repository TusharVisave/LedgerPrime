package com.expensetracker.repository;

import com.expensetracker.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByUserIdAndMonthAndCategory(Long userId, String month, String category);

    List<Budget> findByUserIdAndMonth(Long userId, String month);

    List<Budget> findByRolloverTrue();

    List<Budget> findByUserId(Long userId);
}

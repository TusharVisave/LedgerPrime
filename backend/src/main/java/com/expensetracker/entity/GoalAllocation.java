package com.expensetracker.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "goal_allocations", indexes = @Index(name = "idx_goal_allocations_goal", columnList = "goal_id"))
public class GoalAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Column(length = 200, nullable = false)
    private String note = "";

    @Column(nullable = false)
    private LocalDate date;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public GoalAllocation() {
    }

    public GoalAllocation(Goal goal, long amountPaise, String note, LocalDate date) {
        this.goal = goal;
        this.amountPaise = amountPaise;
        this.note = note == null ? "" : note;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public Goal getGoal() {
        return goal;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public String getNote() {
        return note;
    }

    public LocalDate getDate() {
        return date;
    }
}

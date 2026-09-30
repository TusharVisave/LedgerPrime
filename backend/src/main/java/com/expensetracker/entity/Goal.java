package com.expensetracker.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goals", indexes = @Index(name = "idx_goals_user", columnList = "user_id"))
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "target_paise", nullable = false)
    private long targetPaise;

    private LocalDate deadline;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoalAllocation> allocations = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Goal() {
    }

    public Goal(User user, String name, long targetPaise, LocalDate deadline) {
        this.user = user;
        this.name = name;
        this.targetPaise = targetPaise;
        this.deadline = deadline;
    }

    public long getAllocatedPaise() {
        return allocations.stream().mapToLong(GoalAllocation::getAmountPaise).sum();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTargetPaise() {
        return targetPaise;
    }

    public void setTargetPaise(long targetPaise) {
        this.targetPaise = targetPaise;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public List<GoalAllocation> getAllocations() {
        return allocations;
    }
}

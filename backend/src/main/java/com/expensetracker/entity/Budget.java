package com.expensetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "budgets",
        uniqueConstraints = @UniqueConstraint(name = "uq_budgets_user_month_category",
                columnNames = {"user_id", "budget_month", "category"}))
@Getter
@Setter
@NoArgsConstructor
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Month in yyyy-MM format. Column renamed from "month" (reserved SQL keyword). */
    @Column(name = "budget_month", nullable = false, length = 7)
    private String month;

    /** Money stored as integer minor units (paise). */
    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    /**
     * Optional category this budget applies to. Empty string means the overall
     * monthly budget. Part of the unique key (user, month, category).
     */
    @Column(nullable = false, length = 60)
    private String category = "";

    /** When true, unused budget rolls into next month via the scheduled job. */
    @Column(nullable = false)
    private boolean rollover = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

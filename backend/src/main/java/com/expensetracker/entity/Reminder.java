package com.expensetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reminders")
@Getter
@Setter
@NoArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String title;

    /** Money stored as integer minor units (paise). */
    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Column(nullable = false, length = 60)
    private String category = "Bills";

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "is_recurring", nullable = false)
    private boolean isRecurring = false;

    @Column(nullable = false, length = 20)
    private String recurrence = "monthly";

    @Column(name = "email_notify", nullable = false)
    private boolean emailNotify = false;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    /** Last time an upcoming-due email was sent; prevents duplicate daily emails. */
    @Column(name = "last_notified_at")
    private Instant lastNotifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

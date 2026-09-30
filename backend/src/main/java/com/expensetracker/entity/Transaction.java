package com.expensetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    /** Money stored as integer minor units (paise). Never float/double. */
    @Column(name = "amount_paise", nullable = false)
    private long amountPaise;

    @Column(nullable = false, length = 60)
    private String category;

    @ElementCollection
    @CollectionTable(name = "transaction_tags",
            joinColumns = @JoinColumn(name = "transaction_id"))
    @Column(name = "tag", length = 30)
    private List<String> tags = new ArrayList<>();

    @Column(nullable = false, length = 500)
    private String note = "";

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "recurring_generated", nullable = false)
    private boolean recurringGenerated = false;

    @Column(name = "recurring_source_id")
    private Long recurringSourceId;

    /** Filename of an attached receipt image, stored under the receipts dir. Nullable. */
    @Column(name = "receipt_image_path", length = 500)
    private String receiptImagePath;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

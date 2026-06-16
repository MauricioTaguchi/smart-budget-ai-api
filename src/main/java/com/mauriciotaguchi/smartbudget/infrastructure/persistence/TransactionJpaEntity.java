package com.mauriciotaguchi.smartbudget.infrastructure.persistence;

import com.mauriciotaguchi.smartbudget.domain.model.FinancialTransaction;
import com.mauriciotaguchi.smartbudget.domain.model.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "financial_transactions")
public class TransactionJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "transaction_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected TransactionJpaEntity() {
    }

    private TransactionJpaEntity(
            UUID id,
            TransactionType type,
            BigDecimal amount,
            String description,
            String category,
            LocalDateTime occurredAt,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.category = category;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    public static TransactionJpaEntity fromDomain(FinancialTransaction transaction) {
        return new TransactionJpaEntity(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getCategory(),
                transaction.getOccurredAt(),
                transaction.getCreatedAt()
        );
    }

    public FinancialTransaction toDomain() {
        return new FinancialTransaction(
                id,
                type,
                amount,
                description,
                category,
                occurredAt,
                createdAt
        );
    }
}

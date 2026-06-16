package com.mauriciotaguchi.smartbudget.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class FinancialTransaction {

    private final UUID id;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String description;
    private final String category;
    private final LocalDateTime occurredAt;
    private final LocalDateTime createdAt;

    public FinancialTransaction(
            UUID id,
            TransactionType type,
            BigDecimal amount,
            String description,
            String category,
            LocalDateTime occurredAt,
            LocalDateTime createdAt
    ) {
        this.id = Objects.requireNonNull(id, "O identificador é obrigatório.");
        this.type = Objects.requireNonNull(type, "O tipo é obrigatório.");

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("A descrição é obrigatória.");
        }

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("A categoria é obrigatória.");
        }

        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.description = description.trim();
        this.category = category.trim().toUpperCase();
        this.occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public static FinancialTransaction create(
            TransactionType type,
            BigDecimal amount,
            String description,
            String category
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new FinancialTransaction(
                UUID.randomUUID(),
                type,
                amount,
                description,
                category,
                now,
                now
        );
    }

    public UUID getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

package com.mauriciotaguchi.smartbudget.application.dto;

import com.mauriciotaguchi.smartbudget.domain.model.FinancialTransaction;
import com.mauriciotaguchi.smartbudget.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionView(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        String description,
        String category,
        LocalDateTime occurredAt
) {
    public static TransactionView from(FinancialTransaction transaction) {
        return new TransactionView(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getCategory(),
                transaction.getOccurredAt()
        );
    }
}

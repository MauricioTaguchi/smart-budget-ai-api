package com.mauriciotaguchi.smartbudget.application.dto;

import com.mauriciotaguchi.smartbudget.domain.model.TransactionType;

import java.math.BigDecimal;

public record CreateTransactionCommand(
        TransactionType type,
        BigDecimal amount,
        String description,
        String category
) {
}

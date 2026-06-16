package com.mauriciotaguchi.smartbudget.application.dto;

import java.math.BigDecimal;

public record FinancialSummary(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        long transactionCount
) {
}

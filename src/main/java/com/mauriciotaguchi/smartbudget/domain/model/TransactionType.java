package com.mauriciotaguchi.smartbudget.domain.model;

import java.text.Normalizer;
import java.util.Locale;

public enum TransactionType {
    INCOME,
    EXPENSE;

    public static TransactionType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("O tipo da transação é obrigatório.");
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "INCOME", "RECEITA", "ENTRADA", "GANHO" -> INCOME;
            case "EXPENSE", "DESPESA", "SAIDA", "GASTO" -> EXPENSE;
            default -> throw new IllegalArgumentException(
                    "Tipo inválido. Use INCOME/RECEITA ou EXPENSE/DESPESA."
            );
        };
    }
}

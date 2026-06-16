package com.mauriciotaguchi.smartbudget.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        @NotBlank(message = "O tipo é obrigatório")
        String type,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        BigDecimal amount,

        @NotBlank(message = "A descrição é obrigatória")
        String description,

        @NotBlank(message = "A categoria é obrigatória")
        String category
) {
}

package com.mauriciotaguchi.smartbudget.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextCommandRequest(
        @NotBlank(message = "A mensagem é obrigatória")
        @Size(max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres")
        String message
) {
}

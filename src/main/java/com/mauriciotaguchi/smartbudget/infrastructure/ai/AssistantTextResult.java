package com.mauriciotaguchi.smartbudget.infrastructure.ai;

public record AssistantTextResult(
        String transcript,
        String answer,
        String mode
) {
}

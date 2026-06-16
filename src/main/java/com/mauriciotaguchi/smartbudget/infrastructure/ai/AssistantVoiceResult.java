package com.mauriciotaguchi.smartbudget.infrastructure.ai;

public record AssistantVoiceResult(
        String transcript,
        String answer,
        byte[] audio,
        String mode
) {
}

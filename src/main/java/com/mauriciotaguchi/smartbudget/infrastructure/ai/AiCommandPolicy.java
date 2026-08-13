package com.mauriciotaguchi.smartbudget.infrastructure.ai;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Component
public class AiCommandPolicy {

    private static final int MAX_MESSAGE_LENGTH = 2_000;
    private static final List<String> PROMPT_INJECTION_MARKERS = List.of(
            "ignore as instrucoes",
            "ignore instrucoes anteriores",
            "revele o prompt",
            "mostre o system prompt",
            "developer message",
            "bypass",
            "jailbreak"
    );

    public void validate(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("O comando não pode estar vazio.");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("O comando excede o limite de 2.000 caracteres.");
        }

        String normalized = normalize(message);
        if (PROMPT_INJECTION_MARKERS.stream().anyMatch(normalized::contains)) {
            throw new IllegalArgumentException("O comando contém instruções não permitidas.");
        }
    }

    public boolean isConfirmed(String message) {
        String normalized = normalize(message);
        return normalized.contains("confirmar") || normalized.contains("confirmo");
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}

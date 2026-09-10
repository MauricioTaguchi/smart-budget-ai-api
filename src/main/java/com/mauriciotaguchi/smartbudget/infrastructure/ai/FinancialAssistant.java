package com.mauriciotaguchi.smartbudget.infrastructure.ai;

import org.springframework.core.io.Resource;
import java.util.UUID;

public interface FinancialAssistant {

    AssistantTextResult processText(UUID ownerId, String message);

    AssistantVoiceResult processVoice(UUID ownerId, Resource audio);
}

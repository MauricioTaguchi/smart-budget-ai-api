package com.mauriciotaguchi.smartbudget.infrastructure.ai;

import org.springframework.core.io.Resource;

public interface FinancialAssistant {

    AssistantTextResult processText(String message);

    AssistantVoiceResult processVoice(Resource audio);
}

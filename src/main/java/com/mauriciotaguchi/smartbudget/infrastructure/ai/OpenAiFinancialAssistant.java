package com.mauriciotaguchi.smartbudget.infrastructure.ai;

import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Profile("openai")
public class OpenAiFinancialAssistant implements FinancialAssistant {

    private static final String SYSTEM_PROMPT = """
            Você é um assistente financeiro brasileiro.
            Interprete comandos em português e use as ferramentas disponíveis
            para executar operações reais na aplicação.

            Regras:
            - Nunca invente uma transação.
            - Só registre quando houver tipo, valor, descrição e categoria.
            - Use INCOME para receitas e EXPENSE para despesas.
            - Valores devem ser positivos.
            - Após usar uma ferramenta, responda de forma curta, clara e em português.
            - Para consultas, use as ferramentas em vez de estimar valores.
            """;

    private final ChatClient chatClient;
    private final TranscriptionModel transcriptionModel;
    private final TextToSpeechModel textToSpeechModel;
    private final AiAuditService auditService;

    public OpenAiFinancialAssistant(
            ChatClient.Builder chatClientBuilder,
            FinancialTools financialTools,
            TranscriptionModel transcriptionModel,
            TextToSpeechModel textToSpeechModel,
            AiAuditService auditService
    ) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(financialTools)
                .build();
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
        this.auditService = auditService;
    }

    @Override
    public AssistantTextResult processText(String message) {
        try {
            String answer = generateAnswer(message);
            auditService.success("TEXT", message, answer);
            return new AssistantTextResult(message, answer, "openai");
        } catch (RuntimeException exception) {
            auditService.failure("TEXT", message, exception);
            throw exception;
        }
    }

    @Override
    public AssistantVoiceResult processVoice(Resource audio) {
        String transcript = "";

        try {
            transcript = transcriptionModel.transcribe(audio);
            String answer = generateAnswer(transcript);
            byte[] generatedAudio = textToSpeechModel.call(answer);

            auditService.success("VOICE", transcript, answer);
            return new AssistantVoiceResult(
                    transcript,
                    answer,
                    generatedAudio,
                    "openai"
            );
        } catch (RuntimeException exception) {
            auditService.failure("VOICE", transcript, exception);
            throw exception;
        }
    }

    private String generateAnswer(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("O comando não pode estar vazio.");
        }

        String answer = chatClient.prompt(message)
                .call()
                .content();

        if (answer == null || answer.isBlank()) {
            return "A operação foi processada, mas o modelo não gerou uma resposta.";
        }

        return answer;
    }
}

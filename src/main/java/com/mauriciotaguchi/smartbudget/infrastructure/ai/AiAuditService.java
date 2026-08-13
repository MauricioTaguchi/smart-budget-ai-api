package com.mauriciotaguchi.smartbudget.infrastructure.ai;

import com.mauriciotaguchi.smartbudget.infrastructure.persistence.AiInteractionEntity;
import com.mauriciotaguchi.smartbudget.infrastructure.persistence.AiInteractionJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AiAuditService {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
    private static final Pattern CARD = Pattern.compile("(?<!\\d)(?:\\d[ -]*?){13,19}(?!\\d)");

    private final AiInteractionJpaRepository repository;
    private final String mode;

    public AiAuditService(
            AiInteractionJpaRepository repository,
            @Value("${app.ai.mode:demo}") String mode
    ) {
        this.repository = repository;
        this.mode = mode;
    }

    @Transactional
    public void success(String channel, String input, String response) {
        repository.save(new AiInteractionEntity(
                channel,
                mode,
                safe(input),
                safe(response),
                "SUCCESS",
                null,
                LocalDateTime.now()
        ));
    }

    @Transactional
    public void failure(String channel, String input, Exception exception) {
        repository.save(new AiInteractionEntity(
                channel,
                mode,
                safe(input),
                null,
                "ERROR",
                safe(exception.getMessage()),
                LocalDateTime.now()
        ));
    }

    @Transactional(readOnly = true)
    public List<AiAuditView> list() {
        return repository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(AiAuditView::from)
                .toList();
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        String redacted = EMAIL.matcher(value).replaceAll("[EMAIL_REDACTED]");
        redacted = CARD.matcher(redacted).replaceAll("[PAYMENT_DATA_REDACTED]");
        return redacted.length() > 1900 ? redacted.substring(0, 1900) : redacted;
    }

    public record AiAuditView(
            Long id,
            String channel,
            String mode,
            String input,
            String response,
            String status,
            String error,
            LocalDateTime createdAt
    ) {
        static AiAuditView from(AiInteractionEntity entity) {
            return new AiAuditView(
                    entity.getId(),
                    entity.getChannel(),
                    entity.getMode(),
                    entity.getInputText(),
                    entity.getResponseText(),
                    entity.getStatus(),
                    entity.getErrorMessage(),
                    entity.getCreatedAt()
            );
        }
    }
}

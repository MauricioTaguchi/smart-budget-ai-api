package com.mauriciotaguchi.smartbudget.infrastructure.ai;

import com.mauriciotaguchi.smartbudget.infrastructure.persistence.AiInteractionEntity;
import com.mauriciotaguchi.smartbudget.infrastructure.persistence.AiInteractionJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiAuditService {

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
        return value.length() > 1900 ? value.substring(0, 1900) : value;
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

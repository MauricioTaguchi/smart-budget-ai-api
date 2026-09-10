package com.mauriciotaguchi.smartbudget.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

@Entity
@Table(name = "ai_interactions")
public class AiInteractionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false, length = 20)
    private String mode;

    @Column(name = "input_text", nullable = false, length = 2000)
    private String inputText;

    @Column(name = "response_text", length = 4000)
    private String responseText;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AiInteractionEntity() {
    }

    public AiInteractionEntity(
            UUID ownerId,
            String channel,
            String mode,
            String inputText,
            String responseText,
            String status,
            String errorMessage,
            LocalDateTime createdAt
    ) {
        this.ownerId = Objects.requireNonNull(ownerId, "Proprietário obrigatório.");
        this.channel = channel;
        this.mode = mode;
        this.inputText = inputText;
        this.responseText = responseText;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getChannel() {
        return channel;
    }

    public String getMode() {
        return mode;
    }

    public String getInputText() {
        return inputText;
    }

    public String getResponseText() {
        return responseText;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

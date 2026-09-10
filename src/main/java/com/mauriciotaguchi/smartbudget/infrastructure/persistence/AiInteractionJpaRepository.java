package com.mauriciotaguchi.smartbudget.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiInteractionJpaRepository
        extends JpaRepository<AiInteractionEntity, Long> {

    List<AiInteractionEntity> findTop100ByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}

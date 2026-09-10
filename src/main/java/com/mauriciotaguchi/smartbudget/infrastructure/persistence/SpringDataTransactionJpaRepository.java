package com.mauriciotaguchi.smartbudget.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataTransactionJpaRepository
        extends JpaRepository<TransactionJpaEntity, UUID> {
    List<TransactionJpaEntity> findAllByOwnerIdOrderByOccurredAtDesc(UUID ownerId);
    Optional<TransactionJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Modifying
    @Query("delete from TransactionJpaEntity t where t.id = :id and t.ownerId = :ownerId")
    int deleteOwned(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}

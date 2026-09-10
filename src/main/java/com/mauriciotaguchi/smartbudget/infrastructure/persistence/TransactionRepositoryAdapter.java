package com.mauriciotaguchi.smartbudget.infrastructure.persistence;

import com.mauriciotaguchi.smartbudget.domain.model.FinancialTransaction;
import com.mauriciotaguchi.smartbudget.domain.repository.TransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final SpringDataTransactionJpaRepository repository;

    public TransactionRepositoryAdapter(SpringDataTransactionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public FinancialTransaction save(UUID ownerId, FinancialTransaction transaction) {
        return repository.save(TransactionJpaEntity.fromDomain(ownerId, transaction)).toDomain();
    }

    @Override
    public List<FinancialTransaction> findAllByOwnerId(UUID ownerId) {
        java.util.Objects.requireNonNull(ownerId, "Proprietário obrigatório.");
        return repository.findAllByOwnerIdOrderByOccurredAtDesc(ownerId)
                .stream()
                .map(TransactionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<FinancialTransaction> findByIdAndOwnerId(UUID id, UUID ownerId) {
        java.util.Objects.requireNonNull(ownerId, "Proprietário obrigatório.");
        return repository.findByIdAndOwnerId(id, ownerId).map(TransactionJpaEntity::toDomain);
    }

    @Override
    public int deleteByIdAndOwnerId(UUID id, UUID ownerId) {
        java.util.Objects.requireNonNull(ownerId, "Proprietário obrigatório.");
        return repository.deleteOwned(id, ownerId);
    }
}

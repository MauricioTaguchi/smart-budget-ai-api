package com.mauriciotaguchi.smartbudget.infrastructure.persistence;

import com.mauriciotaguchi.smartbudget.domain.model.FinancialTransaction;
import com.mauriciotaguchi.smartbudget.domain.repository.TransactionRepository;
import org.springframework.data.domain.Sort;
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
    public FinancialTransaction save(FinancialTransaction transaction) {
        return repository.save(TransactionJpaEntity.fromDomain(transaction)).toDomain();
    }

    @Override
    public List<FinancialTransaction> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "occurredAt"))
                .stream()
                .map(TransactionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<FinancialTransaction> findById(UUID id) {
        return repository.findById(id).map(TransactionJpaEntity::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }
}

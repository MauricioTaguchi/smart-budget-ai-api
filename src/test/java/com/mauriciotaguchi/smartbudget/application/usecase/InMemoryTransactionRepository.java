package com.mauriciotaguchi.smartbudget.application.usecase;

import com.mauriciotaguchi.smartbudget.domain.model.FinancialTransaction;
import com.mauriciotaguchi.smartbudget.domain.repository.TransactionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<UUID, List<FinancialTransaction>> byOwner = new HashMap<>();

    @Override
    public FinancialTransaction save(UUID ownerId, FinancialTransaction transaction) {
        var transactions = byOwner.computeIfAbsent(Objects.requireNonNull(ownerId), ignored -> new ArrayList<>());
        transactions.removeIf(item -> item.getId().equals(transaction.getId()));
        transactions.add(transaction);
        return transaction;
    }

    @Override
    public List<FinancialTransaction> findAllByOwnerId(UUID ownerId) {
        return List.copyOf(byOwner.getOrDefault(Objects.requireNonNull(ownerId), List.of()));
    }

    @Override
    public Optional<FinancialTransaction> findByIdAndOwnerId(UUID id, UUID ownerId) {
        return findAllByOwnerId(ownerId).stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
    }

    @Override
    public int deleteByIdAndOwnerId(UUID id, UUID ownerId) {
        List<FinancialTransaction> rows = byOwner.get(Objects.requireNonNull(ownerId));
        return rows != null && rows.removeIf(item -> item.getId().equals(id)) ? 1 : 0;
    }
}

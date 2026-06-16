package com.mauriciotaguchi.smartbudget.application.usecase;

import com.mauriciotaguchi.smartbudget.domain.model.FinancialTransaction;
import com.mauriciotaguchi.smartbudget.domain.repository.TransactionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class InMemoryTransactionRepository implements TransactionRepository {

    private final List<FinancialTransaction> transactions = new ArrayList<>();

    @Override
    public FinancialTransaction save(FinancialTransaction transaction) {
        transactions.removeIf(item -> item.getId().equals(transaction.getId()));
        transactions.add(transaction);
        return transaction;
    }

    @Override
    public List<FinancialTransaction> findAll() {
        return List.copyOf(transactions);
    }

    @Override
    public Optional<FinancialTransaction> findById(UUID id) {
        return transactions.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
    }

    @Override
    public void deleteById(UUID id) {
        transactions.removeIf(item -> item.getId().equals(id));
    }

    @Override
    public boolean existsById(UUID id) {
        return transactions.stream()
                .anyMatch(item -> item.getId().equals(id));
    }
}

package com.mauriciotaguchi.smartbudget.domain.repository;

import com.mauriciotaguchi.smartbudget.domain.model.FinancialTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {

    FinancialTransaction save(FinancialTransaction transaction);

    List<FinancialTransaction> findAll();

    Optional<FinancialTransaction> findById(UUID id);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}

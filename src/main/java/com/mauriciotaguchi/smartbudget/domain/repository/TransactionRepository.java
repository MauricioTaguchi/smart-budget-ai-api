package com.mauriciotaguchi.smartbudget.domain.repository;

import com.mauriciotaguchi.smartbudget.domain.model.FinancialTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {

    FinancialTransaction save(UUID ownerId, FinancialTransaction transaction);

    List<FinancialTransaction> findAllByOwnerId(UUID ownerId);

    Optional<FinancialTransaction> findByIdAndOwnerId(UUID id, UUID ownerId);

    int deleteByIdAndOwnerId(UUID id, UUID ownerId);
}

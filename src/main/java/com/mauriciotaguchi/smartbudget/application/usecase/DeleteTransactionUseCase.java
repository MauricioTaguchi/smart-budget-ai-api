package com.mauriciotaguchi.smartbudget.application.usecase;

import com.mauriciotaguchi.smartbudget.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteTransactionUseCase {

    private final TransactionRepository repository;

    public DeleteTransactionUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(UUID id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Transação não encontrada: " + id);
        }

        repository.deleteById(id);
    }
}

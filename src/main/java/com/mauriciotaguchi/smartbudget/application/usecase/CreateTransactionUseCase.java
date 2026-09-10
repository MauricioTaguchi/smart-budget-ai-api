package com.mauriciotaguchi.smartbudget.application.usecase;

import com.mauriciotaguchi.smartbudget.application.dto.CreateTransactionCommand;
import com.mauriciotaguchi.smartbudget.application.dto.TransactionView;
import com.mauriciotaguchi.smartbudget.domain.model.FinancialTransaction;
import com.mauriciotaguchi.smartbudget.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.Objects;

@Service
public class CreateTransactionUseCase {

    private final TransactionRepository repository;

    public CreateTransactionUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TransactionView execute(UUID ownerId, CreateTransactionCommand command) {
        Objects.requireNonNull(ownerId, "Proprietário obrigatório.");
        FinancialTransaction transaction = FinancialTransaction.create(
                command.type(),
                command.amount(),
                command.description(),
                command.category()
        );

        return TransactionView.from(repository.save(ownerId, transaction));
    }
}

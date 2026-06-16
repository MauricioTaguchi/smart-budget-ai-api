package com.mauriciotaguchi.smartbudget.application.usecase;

import com.mauriciotaguchi.smartbudget.application.dto.CreateTransactionCommand;
import com.mauriciotaguchi.smartbudget.application.dto.TransactionView;
import com.mauriciotaguchi.smartbudget.domain.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateTransactionUseCaseTest {

    @Test
    void shouldCreateAnExpense() {
        InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
        CreateTransactionUseCase useCase = new CreateTransactionUseCase(repository);

        TransactionView result = useCase.execute(new CreateTransactionCommand(
                TransactionType.EXPENSE,
                new BigDecimal("89.90"),
                "Supermercado",
                "Alimentação"
        ));

        assertEquals(TransactionType.EXPENSE, result.type());
        assertEquals(new BigDecimal("89.90"), result.amount());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void shouldRejectZeroAmount() {
        InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
        CreateTransactionUseCase useCase = new CreateTransactionUseCase(repository);

        assertThrows(IllegalArgumentException.class, () ->
                useCase.execute(new CreateTransactionCommand(
                        TransactionType.EXPENSE,
                        BigDecimal.ZERO,
                        "Compra inválida",
                        "Outros"
                ))
        );
    }
}

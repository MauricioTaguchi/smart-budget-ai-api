package com.mauriciotaguchi.smartbudget.application.usecase;

import com.mauriciotaguchi.smartbudget.application.dto.CreateTransactionCommand;
import com.mauriciotaguchi.smartbudget.application.dto.FinancialSummary;
import com.mauriciotaguchi.smartbudget.domain.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetFinancialSummaryUseCaseTest {

    @Test
    void shouldCalculateIncomeExpenseAndBalance() {
        InMemoryTransactionRepository repository = new InMemoryTransactionRepository();
        CreateTransactionUseCase create = new CreateTransactionUseCase(repository);
        GetFinancialSummaryUseCase summaryUseCase =
                new GetFinancialSummaryUseCase(repository);

        create.execute(new CreateTransactionCommand(
                TransactionType.INCOME,
                new BigDecimal("3000.00"),
                "Salário",
                "Trabalho"
        ));

        create.execute(new CreateTransactionCommand(
                TransactionType.EXPENSE,
                new BigDecimal("500.00"),
                "Aluguel",
                "Moradia"
        ));

        FinancialSummary summary = summaryUseCase.execute();

        assertEquals(new BigDecimal("3000.00"), summary.totalIncome());
        assertEquals(new BigDecimal("500.00"), summary.totalExpense());
        assertEquals(new BigDecimal("2500.00"), summary.balance());
        assertEquals(2, summary.transactionCount());
    }
}

package com.mauriciotaguchi.smartbudget.infrastructure.ai;

import com.mauriciotaguchi.smartbudget.application.usecase.CreateTransactionUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.DeleteTransactionUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.GetFinancialSummaryUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.ListTransactionsUseCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class FinancialToolsTest {

    private final CreateTransactionUseCase create = mock(CreateTransactionUseCase.class);
    private final DeleteTransactionUseCase delete = mock(DeleteTransactionUseCase.class);
    private final FinancialTools tools = new FinancialTools(
            create,
            mock(ListTransactionsUseCase.class),
            mock(GetFinancialSummaryUseCase.class),
            delete
    );

    @Test
    void refusesMutationWithoutConfirmation() {
        assertThatThrownBy(() -> tools.registerTransaction(
                "EXPENSE", BigDecimal.TEN, "Café", "ALIMENTACAO", false
        )).hasMessageContaining("confirmação explícita");

        assertThatThrownBy(() -> tools.deleteFinancialTransaction(
                "6f9619ff-8b86-d011-b42d-00cf4fc964ff", false
        )).hasMessageContaining("confirmação explícita");

        verifyNoInteractions(create, delete);
    }
}

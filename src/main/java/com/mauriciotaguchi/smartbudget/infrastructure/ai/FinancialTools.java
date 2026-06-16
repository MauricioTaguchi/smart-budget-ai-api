package com.mauriciotaguchi.smartbudget.infrastructure.ai;

import com.mauriciotaguchi.smartbudget.application.dto.CreateTransactionCommand;
import com.mauriciotaguchi.smartbudget.application.dto.FinancialSummary;
import com.mauriciotaguchi.smartbudget.application.dto.TransactionView;
import com.mauriciotaguchi.smartbudget.application.usecase.CreateTransactionUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.DeleteTransactionUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.GetFinancialSummaryUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.ListTransactionsUseCase;
import com.mauriciotaguchi.smartbudget.domain.model.TransactionType;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class FinancialTools {

    private final CreateTransactionUseCase createTransaction;
    private final ListTransactionsUseCase listTransactions;
    private final GetFinancialSummaryUseCase getSummary;
    private final DeleteTransactionUseCase deleteTransaction;

    public FinancialTools(
            CreateTransactionUseCase createTransaction,
            ListTransactionsUseCase listTransactions,
            GetFinancialSummaryUseCase getSummary,
            DeleteTransactionUseCase deleteTransaction
    ) {
        this.createTransaction = createTransaction;
        this.listTransactions = listTransactions;
        this.getSummary = getSummary;
        this.deleteTransaction = deleteTransaction;
    }

    @Tool(description = """
            Registra uma transação financeira real no sistema.
            Use INCOME para receita e EXPENSE para despesa.
            O valor deve ser positivo.
            """)
    public TransactionView registerTransaction(
            @ToolParam(description = "Tipo: INCOME ou EXPENSE") String type,
            @ToolParam(description = "Valor positivo da transação") BigDecimal amount,
            @ToolParam(description = "Descrição objetiva da transação") String description,
            @ToolParam(description = "Categoria financeira") String category
    ) {
        return createTransaction.execute(new CreateTransactionCommand(
                TransactionType.from(type),
                amount,
                description,
                category
        ));
    }

    @Tool(description = "Consulta o resumo financeiro com receitas, despesas, saldo e quantidade de transações.")
    public FinancialSummary getFinancialSummary() {
        return getSummary.execute();
    }

    @Tool(description = "Lista as transações financeiras cadastradas, da mais recente para a mais antiga.")
    public List<TransactionView> listFinancialTransactions() {
        return listTransactions.execute();
    }

    @Tool(description = "Exclui uma transação financeira pelo identificador UUID.")
    public String deleteFinancialTransaction(
            @ToolParam(description = "UUID da transação") String transactionId
    ) {
        deleteTransaction.execute(UUID.fromString(transactionId));
        return "Transação excluída com sucesso.";
    }
}

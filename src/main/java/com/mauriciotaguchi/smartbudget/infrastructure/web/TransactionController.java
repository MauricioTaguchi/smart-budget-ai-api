package com.mauriciotaguchi.smartbudget.infrastructure.web;

import com.mauriciotaguchi.smartbudget.application.dto.CreateTransactionCommand;
import com.mauriciotaguchi.smartbudget.application.dto.FinancialSummary;
import com.mauriciotaguchi.smartbudget.application.dto.TransactionView;
import com.mauriciotaguchi.smartbudget.application.usecase.CreateTransactionUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.DeleteTransactionUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.GetFinancialSummaryUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.ListTransactionsUseCase;
import com.mauriciotaguchi.smartbudget.domain.model.TransactionType;
import com.mauriciotaguchi.smartbudget.infrastructure.web.dto.CreateTransactionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final CreateTransactionUseCase createTransaction;
    private final ListTransactionsUseCase listTransactions;
    private final GetFinancialSummaryUseCase getSummary;
    private final DeleteTransactionUseCase deleteTransaction;

    public TransactionController(
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionView create(@Valid @RequestBody CreateTransactionRequest request) {
        return createTransaction.execute(new CreateTransactionCommand(
                TransactionType.from(request.type()),
                request.amount(),
                request.description(),
                request.category()
        ));
    }

    @GetMapping
    public List<TransactionView> list() {
        return listTransactions.execute();
    }

    @GetMapping("/summary")
    public FinancialSummary summary() {
        return getSummary.execute();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteTransaction.execute(id);
    }
}

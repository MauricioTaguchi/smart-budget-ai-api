package com.mauriciotaguchi.smartbudget.infrastructure.ai;

import com.mauriciotaguchi.smartbudget.application.usecase.*;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class FinancialToolsFactory {
    private final CreateTransactionUseCase create;
    private final ListTransactionsUseCase list;
    private final GetFinancialSummaryUseCase summary;
    private final DeleteTransactionUseCase delete;

    public FinancialToolsFactory(CreateTransactionUseCase create, ListTransactionsUseCase list,
                                 GetFinancialSummaryUseCase summary, DeleteTransactionUseCase delete) {
        this.create = create;
        this.list = list;
        this.summary = summary;
        this.delete = delete;
    }

    public FinancialTools forOwner(UUID trustedOwnerId) {
        return new FinancialTools(trustedOwnerId, create, list, summary, delete);
    }
}

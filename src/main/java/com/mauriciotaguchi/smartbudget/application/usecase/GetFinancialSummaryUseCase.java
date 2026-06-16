package com.mauriciotaguchi.smartbudget.application.usecase;

import com.mauriciotaguchi.smartbudget.application.dto.FinancialSummary;
import com.mauriciotaguchi.smartbudget.domain.model.FinancialTransaction;
import com.mauriciotaguchi.smartbudget.domain.model.TransactionType;
import com.mauriciotaguchi.smartbudget.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GetFinancialSummaryUseCase {

    private final TransactionRepository repository;

    public GetFinancialSummaryUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public FinancialSummary execute() {
        List<FinancialTransaction> transactions = repository.findAll();

        BigDecimal income = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.INCOME)
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expense = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FinancialSummary(
                income,
                expense,
                income.subtract(expense),
                transactions.size()
        );
    }
}

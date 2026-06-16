package com.mauriciotaguchi.smartbudget.application.usecase;

import com.mauriciotaguchi.smartbudget.application.dto.TransactionView;
import com.mauriciotaguchi.smartbudget.domain.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListTransactionsUseCase {

    private final TransactionRepository repository;

    public ListTransactionsUseCase(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TransactionView> execute() {
        return repository.findAll().stream()
                .map(TransactionView::from)
                .toList();
    }
}

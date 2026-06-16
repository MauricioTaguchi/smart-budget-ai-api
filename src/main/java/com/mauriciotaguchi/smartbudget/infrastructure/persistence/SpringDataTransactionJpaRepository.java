package com.mauriciotaguchi.smartbudget.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataTransactionJpaRepository
        extends JpaRepository<TransactionJpaEntity, UUID> {
}

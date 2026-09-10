package com.mauriciotaguchi.smartbudget.infrastructure.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthPrincipalRepository extends JpaRepository<AuthPrincipalEntity, UUID> {
    Optional<AuthPrincipalEntity> findByTokenHashAndActiveTrue(String tokenHash);
    boolean existsByUsername(String username);
    long countByRoleAndActiveTrue(PrincipalRole role);
    List<AuthPrincipalEntity> findAllByOrderByCreatedAtAsc();

    @Query(value = "select id from auth_administration_lock where id = 1 for update", nativeQuery = true)
    Integer lockAdministration();
}


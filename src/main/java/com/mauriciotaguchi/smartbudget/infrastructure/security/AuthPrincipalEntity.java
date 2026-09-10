package com.mauriciotaguchi.smartbudget.infrastructure.security;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_principals")
public class AuthPrincipalEntity {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 64)
    private String username;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PrincipalRole role;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "token_hash", unique = true, length = 64)
    private String tokenHash;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuthPrincipalEntity() {}

    public AuthPrincipalEntity(String username, PrincipalRole role, String tokenHash) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.role = role;
        this.active = true;
        this.tokenHash = tokenHash;
        this.createdAt = Instant.now();
    }

    public void rotateToken(String tokenHash) {
        this.tokenHash = tokenHash;
        this.active = true;
    }

    public void revoke() {
        this.tokenHash = null;
        this.active = false;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public PrincipalRole getRole() { return role; }
    public boolean isActive() { return active; }
    public String getTokenHash() { return tokenHash; }
    public Instant getCreatedAt() { return createdAt; }
}


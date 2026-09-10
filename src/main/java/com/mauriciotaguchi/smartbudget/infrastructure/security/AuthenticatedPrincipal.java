package com.mauriciotaguchi.smartbudget.infrastructure.security;

import java.security.Principal;
import java.util.UUID;

public record AuthenticatedPrincipal(UUID id, String username, PrincipalRole role, String credentialHash)
        implements Principal {
    @Override
    public String getName() { return username; }

    @Override
    public String toString() {
        return "AuthenticatedPrincipal[id=" + id + ", role=" + role + "]";
    }
}


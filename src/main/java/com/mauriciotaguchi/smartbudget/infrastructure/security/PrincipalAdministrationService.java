package com.mauriciotaguchi.smartbudget.infrastructure.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PrincipalAdministrationService {
    private final AuthPrincipalRepository principals;
    private final OpaqueTokenService tokens;

    public PrincipalAdministrationService(AuthPrincipalRepository principals, OpaqueTokenService tokens) {
        this.principals = principals;
        this.tokens = tokens;
    }

    @Transactional
    public CredentialView create(AuthenticatedPrincipal actor, String username, PrincipalRole role) {
        lockAndCheckAdministrator(actor);
        validateUsername(username);
        if (role == null) throw new IllegalArgumentException("O papel é obrigatório.");
        if (principals.existsByUsername(username))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Identificador já cadastrado.");
        String token = tokens.generate();
        AuthPrincipalEntity entity = principals.save(new AuthPrincipalEntity(username, role, tokens.hash(token)));
        return CredentialView.from(entity, token);
    }

    @Transactional(readOnly = true)
    public List<PrincipalView> list(AuthenticatedPrincipal actor) {
        requireAdministrator(actor);
        return principals.findAllByOrderByCreatedAtAsc().stream().map(PrincipalView::from).toList();
    }

    @Transactional
    public CredentialView rotate(AuthenticatedPrincipal actor, UUID id) {
        lockAndCheckAdministrator(actor);
        AuthPrincipalEntity entity = find(id);
        String token = tokens.generate();
        entity.rotateToken(tokens.hash(token));
        return CredentialView.from(entity, token);
    }

    @Transactional
    public void revoke(AuthenticatedPrincipal actor, UUID id) {
        lockAndCheckAdministrator(actor);
        AuthPrincipalEntity entity = find(id);
        if (actor.id().equals(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Um administrador não pode revogar a própria credencial.");
        if (entity.isActive() && entity.getRole() == PrincipalRole.ADMIN
                && principals.countByRoleAndActiveTrue(PrincipalRole.ADMIN) <= 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mantenha ao menos um administrador ativo.");
        entity.revoke();
    }

    @Transactional
    public void bootstrap(String token, String username) {
        acquireAdministrationLock();
        if (principals.count() > 0) {
            if (principals.countByRoleAndActiveTrue(PrincipalRole.ADMIN) == 0)
                throw new IllegalStateException("Sem administrador ativo. Recuperação manual verificada é necessária; bootstrap não redefine credenciais existentes.");
            return;
        }
        if (!tokens.validFormat(token) || token.chars().distinct().count() < 12)
            throw new IllegalStateException("Primeira instalação exige APP_BOOTSTRAP_ADMIN_TOKEN aleatório de 32 bytes em base64url sem padding.");
        validateUsername(username);
        principals.save(new AuthPrincipalEntity(username, PrincipalRole.ADMIN, tokens.hash(token)));
    }

    private void lockAndCheckAdministrator(AuthenticatedPrincipal actor) {
        acquireAdministrationLock();
        requireAdministrator(actor);
    }

    private void acquireAdministrationLock() {
        if (!Integer.valueOf(1).equals(principals.lockAdministration()))
            throw new IllegalStateException("Bloqueio administrativo indisponível.");
    }

    private void requireAdministrator(AuthenticatedPrincipal actor) {
        if (actor == null) throw new AccessDeniedException("Acesso negado.");
        AuthPrincipalEntity current = principals.findById(actor.id()).orElse(null);
        if (current == null || !current.isActive() || current.getRole() != PrincipalRole.ADMIN
                || !current.getTokenHash().equals(actor.credentialHash()))
            throw new AccessDeniedException("Acesso negado.");
    }

    private AuthPrincipalEntity find(UUID id) {
        return principals.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Identificador não encontrado."));
    }

    static void validateUsername(String username) {
        if (username == null || !username.matches("[A-Za-z0-9][A-Za-z0-9._-]{2,63}"))
            throw new IllegalArgumentException("Use um identificador de 3 a 64 caracteres: letras, números, ponto, hífen ou sublinhado.");
    }

    public record PrincipalView(UUID id, String username, PrincipalRole role, boolean active, Instant createdAt) {
        static PrincipalView from(AuthPrincipalEntity entity) {
            return new PrincipalView(entity.getId(), entity.getUsername(), entity.getRole(),
                    entity.isActive(), entity.getCreatedAt());
        }
    }

    public record CredentialView(UUID id, String username, PrincipalRole role, boolean active, String token) {
        static CredentialView from(AuthPrincipalEntity entity, String token) {
            return new CredentialView(entity.getId(), entity.getUsername(), entity.getRole(), entity.isActive(), token);
        }
        @Override public String toString() { return "CredentialView[id=" + id + ", role=" + role + "]"; }
    }
}

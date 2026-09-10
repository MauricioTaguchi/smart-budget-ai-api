package com.mauriciotaguchi.smartbudget.infrastructure.security;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BootstrapAdministratorTest {
    private final AuthPrincipalRepository principals = mock(AuthPrincipalRepository.class);
    private final OpaqueTokenService tokens = new OpaqueTokenService();
    private final PrincipalAdministrationService service = new PrincipalAdministrationService(principals, tokens);

    @Test
    void firstInstallationRejectsMissingWeakOrNonCanonicalTokenWithoutCreatingAnIdentity() {
        when(principals.lockAdministration()).thenReturn(1);
        for (String invalid : new String[]{"", "change-me", "A".repeat(43), tokens.generate() + "="}) {
            assertThatThrownBy(() -> service.bootstrap(invalid, "bootstrap-admin"))
                    .isInstanceOf(IllegalStateException.class);
        }
        verify(principals, never()).save(any());
    }

    @Test
    void firstInstallationStoresOnlyADigestForAnIndividualAdministrator() {
        when(principals.lockAdministration()).thenReturn(1);
        String token = tokens.generate();
        service.bootstrap(token, "bootstrap-admin");
        verify(principals).save(argThat(entity ->
                entity.getRole() == PrincipalRole.ADMIN && entity.isActive()
                        && entity.getId() != null && entity.getCreatedAt() != null
                        && entity.getTokenHash().equals(tokens.hash(token))
                        && !entity.getTokenHash().equals(token)));
    }

    @Test
    void restartNeverResetsCredentialsAndMissingAdminRequiresManualRecovery() {
        when(principals.lockAdministration()).thenReturn(1);
        when(principals.count()).thenReturn(3L);
        when(principals.countByRoleAndActiveTrue(PrincipalRole.ADMIN)).thenReturn(1L);
        service.bootstrap("", "ignored-on-restart");
        verify(principals, never()).save(any());

        when(principals.countByRoleAndActiveTrue(PrincipalRole.ADMIN)).thenReturn(0L);
        assertThatThrownBy(() -> service.bootstrap(tokens.generate(), "new-admin"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Recuperação manual");
        verify(principals, never()).save(any());
    }

    @Test
    void missingSerializationLockFailsClosed() {
        assertThatThrownBy(() -> service.bootstrap(tokens.generate(), "bootstrap-admin"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Bloqueio administrativo");
        verify(principals, never()).save(any());
    }

    @Test
    void rotatedCredentialCannotPerformAdministrativeMutationFromAnOlderContext() {
        when(principals.lockAdministration()).thenReturn(1);
        String oldToken = tokens.generate();
        var administrator = new AuthPrincipalEntity("admin-account", PrincipalRole.ADMIN, tokens.hash(oldToken));
        var context = new AuthenticatedPrincipal(administrator.getId(), administrator.getUsername(),
                PrincipalRole.ADMIN, administrator.getTokenHash());
        administrator.rotateToken(tokens.hash(tokens.generate()));
        when(principals.findById(administrator.getId())).thenReturn(java.util.Optional.of(administrator));
        assertThatThrownBy(() -> service.create(context, "unauthorized-user", PrincipalRole.USER))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(principals, never()).save(any());
    }
}


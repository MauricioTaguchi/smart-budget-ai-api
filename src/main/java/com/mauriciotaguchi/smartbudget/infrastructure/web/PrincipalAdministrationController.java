package com.mauriciotaguchi.smartbudget.infrastructure.web;

import com.mauriciotaguchi.smartbudget.infrastructure.security.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/principals")
public class PrincipalAdministrationController {
    private final PrincipalAdministrationService administration;

    public PrincipalAdministrationController(PrincipalAdministrationService administration) {
        this.administration = administration;
    }

    @GetMapping
    public ResponseEntity<List<PrincipalAdministrationService.PrincipalView>> list(
            @AuthenticationPrincipal AuthenticatedPrincipal actor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(administration.list(actor));
    }

    @PostMapping
    public ResponseEntity<PrincipalAdministrationService.CredentialView> create(
            @AuthenticationPrincipal AuthenticatedPrincipal actor,
            @Valid @RequestBody CreatePrincipalRequest request) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(administration.create(actor, request.username(), request.role()));
    }

    @PostMapping("/{id}/rotate-token")
    public ResponseEntity<PrincipalAdministrationService.CredentialView> rotate(
            @AuthenticationPrincipal AuthenticatedPrincipal actor, @PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(administration.rotate(actor, id));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal AuthenticatedPrincipal actor, @PathVariable UUID id) {
        administration.revoke(actor, id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    public record CreatePrincipalRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{2,63}") String username,
            @NotNull PrincipalRole role) {}
}


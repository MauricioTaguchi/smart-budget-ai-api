package com.mauriciotaguchi.smartbudget.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class BearerTokenFilter extends OncePerRequestFilter {
    private final AuthPrincipalRepository principals;
    private final OpaqueTokenService tokens;

    public BearerTokenFilter(AuthPrincipalRepository principals, OpaqueTokenService tokens) {
        this.principals = principals;
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        List<String> headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        if (headers.size() != 1 || !headers.get(0).regionMatches(true, 0, "Bearer ", 0, 7)) {
            unauthorized(response);
            return;
        }
        String token = headers.get(0).substring(7);
        if (!tokens.validFormat(token)) {
            unauthorized(response);
            return;
        }
        AuthPrincipalEntity principal = principals.findByTokenHashAndActiveTrue(tokens.hash(token)).orElse(null);
        if (principal == null) {
            unauthorized(response);
            return;
        }
        var identity = new AuthenticatedPrincipal(principal.getId(), principal.getUsername(),
                principal.getRole(), principal.getTokenHash());
        var authentication = new UsernamePasswordAuthenticationToken(identity, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole().name())));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Credencial ausente ou inválida.\"}");
    }
}

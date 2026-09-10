package com.mauriciotaguchi.smartbudget.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdministrator implements ApplicationRunner {
    private final PrincipalAdministrationService administration;
    private final String token;
    private final String username;

    public BootstrapAdministrator(PrincipalAdministrationService administration,
            @Value("${app.security.bootstrap-admin-token:}") String token,
            @Value("${app.security.bootstrap-admin-username:bootstrap-admin}") String username) {
        this.administration = administration;
        this.token = token;
        this.username = username;
    }

    @Override
    public void run(ApplicationArguments args) { administration.bootstrap(token, username); }
}


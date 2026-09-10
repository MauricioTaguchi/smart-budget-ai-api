package com.mauriciotaguchi.smartbudget;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.mauriciotaguchi.smartbudget.infrastructure.security.OpaqueTokenService;

@SpringBootTest
@ActiveProfiles("test")
class SmartBudgetApplicationTests {
    private static final String TOKEN = new OpaqueTokenService().generate();

    @DynamicPropertySource
    static void security(DynamicPropertyRegistry registry) {
        registry.add("app.security.bootstrap-admin-token", () -> TOKEN);
    }

    @Test
    void contextLoads() {
    }
}

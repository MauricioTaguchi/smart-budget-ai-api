package com.mauriciotaguchi.smartbudget.infrastructure.web;

import com.mauriciotaguchi.smartbudget.infrastructure.ai.FinancialAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.h2.console.enabled=false",
        "app.security.enabled=false",
        "app.security.api-key=obsolete-shared-api-key",
        "spring.ai.model.chat=none",
        "spring.ai.model.audio.transcription=none",
        "spring.ai.model.audio.speech=none",
        "spring.ai.model.image=none",
        "spring.ai.model.embedding=none",
        "spring.ai.model.moderation=none",
        "spring.ai.chat.client.enabled=false",
        "spring.ai.openai.api-key=unused-local-test-value",
        "management.endpoints.web.exposure.include=health,info"
})
abstract class ProfileSecurityBoundaryHttpTest {

    private static final String ADMIN_TOKEN = randomToken();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    @LocalServerPort
    private int port;

    @MockitoBean
    private FinancialAssistant assistant;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String database = "jdbc:h2:mem:profile_security_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        registry.add("spring.datasource.url", () -> database);
        registry.add("app.security.bootstrap-admin-token", () -> ADMIN_TOKEN);
    }

    @Test
    void profileCannotDisableIdentityChecksOrReachAiBeforeAuthentication() throws Exception {
        for (String path : List.of("/api/transactions", "/api/transactions/summary",
                "/api/audits", "/api/admin/principals", "/actuator/info")) {
            assertThat(send("GET", path, null, null).statusCode()).as(path).isEqualTo(401);
        }
        assertThat(send("POST", "/api/assistant/text", null,
                "{\"message\":\"private text must never reach the model\"}").statusCode()).isEqualTo(401);
        assertThat(send("POST", "/api/assistant/voice", null, "private audio").statusCode()).isEqualTo(401);
        verifyNoInteractions(assistant);

        HttpRequest obsoleteKey = HttpRequest.newBuilder(uri("/api/transactions"))
                .timeout(Duration.ofSeconds(15)).header("X-API-Key", "obsolete-shared-api-key").GET().build();
        assertThat(CLIENT.send(obsoleteKey, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/actuator/health", null, null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/transactions", ADMIN_TOKEN, null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/admin/principals", ADMIN_TOKEN, null).statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return CLIENT.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

@ActiveProfiles("demo")
class DemoProfileSecurityBoundaryHttpTest extends ProfileSecurityBoundaryHttpTest {
}

@ActiveProfiles("openai")
class OpenAiProfileSecurityBoundaryHttpTest extends ProfileSecurityBoundaryHttpTest {
}

@ActiveProfiles("prod")
class ProductionProfileSecurityBoundaryHttpTest extends ProfileSecurityBoundaryHttpTest {
}

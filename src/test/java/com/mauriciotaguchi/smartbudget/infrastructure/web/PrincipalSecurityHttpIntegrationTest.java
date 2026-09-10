package com.mauriciotaguchi.smartbudget.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.h2.console.enabled=false",
        "app.security.enabled=false",
        "app.security.api-key=obsolete-shared-api-key",
        "management.endpoints.web.exposure.include=health,info"
})
class PrincipalSecurityHttpIntegrationTest {

    private static final String ADMIN_TOKEN = randomToken();
    private static final String DATABASE = "jdbc:h2:mem:security_" + UUID.randomUUID()
            + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> DATABASE);
        registry.add("app.security.bootstrap-admin-token", () -> ADMIN_TOKEN);
    }

    @Test
    void authenticationIsRequiredBeforeApiParsingAndOnlyHealthIsPublic() throws Exception {
        for (String path : List.of("/api/transactions", "/api/transactions/summary",
                "/api/audits", "/api/admin/principals", "/api/does-not-exist", "/actuator/info")) {
            assertStatus(request("GET", path, null, null), 401);
        }
        assertStatus(request("POST", "/api/assistant/text", null, "{}"), 401);
        assertStatus(request("POST", "/api/assistant/voice", null, "not-multipart"), 401);
        assertStatus(request("POST", "/api/transactions", null, "invalid-json"), 401);
        assertStatus(request("GET", "/actuator/health", null, null), 200);
        assertStatus(request("GET", "/actuator/info", ADMIN_TOKEN, null), 200);

        Principal user = newPrincipal("USER");
        assertStatus(request("GET", "/actuator/info", user.token(), null), 403);
    }

    @Test
    void obsoleteApiKeysCookiesQueryParametersAndMalformedTokensCannotAuthenticate() throws Exception {
        for (String authorization : List.of("Basic " + ADMIN_TOKEN, "Bearer short",
                "Bearer " + randomToken(), "Bearer " + ADMIN_TOKEN + "x", "Bearer " + "!".repeat(43))) {
            assertStatus(requestWithHeaders("GET", "/api/transactions", null,
                    Map.of("Authorization", authorization)), 401);
        }
        assertStatus(requestWithHeaders("GET", "/api/transactions", null,
                Map.of("X-API-Key", "obsolete-shared-api-key")), 401);
        assertStatus(requestWithHeaders("GET", "/api/transactions", null,
                Map.of("X-API-Key", ADMIN_TOKEN)), 401);
        assertStatus(requestWithHeaders("GET", "/api/transactions", null,
                Map.of("Cookie", "token=" + ADMIN_TOKEN + "; Authorization=Bearer%20" + ADMIN_TOKEN)), 401);
        assertStatus(request("GET", "/api/transactions?token=" + ADMIN_TOKEN
                + "&access_token=" + ADMIN_TOKEN, null, null), 401);
        assertStatus(request("GET", "/api/transactions", ADMIN_TOKEN, null), 200);
    }

    @Test
    void usersAndAdministratorsSeeOnlyTheirOwnFinancialDataAndCannotDeleteForeignRows() throws Exception {
        Principal alice = newPrincipal("USER");
        Principal bob = newPrincipal("USER");
        Principal administrator = newPrincipal("ADMIN");
        String aliceMarker = "alice-private-" + UUID.randomUUID();
        String bobMarker = "bob-private-" + UUID.randomUUID();
        String adminMarker = "admin-private-" + UUID.randomUUID();
        String aliceTransaction = createTransaction(alice.token(), "INCOME", "101.25", aliceMarker);
        String bobTransaction = createTransaction(bob.token(), "EXPENSE", "31.50", bobMarker);
        createTransaction(administrator.token(), "EXPENSE", "2.00", adminMarker);

        HttpResponse<String> aliceList = request("GET", "/api/transactions", alice.token(), null);
        HttpResponse<String> bobList = request("GET", "/api/transactions", bob.token(), null);
        HttpResponse<String> adminList = request("GET", "/api/transactions", administrator.token(), null);
        assertStatus(aliceList, 200);
        assertStatus(bobList, 200);
        assertStatus(adminList, 200);
        assertThat(aliceList.body()).contains(aliceMarker).doesNotContain(bobMarker, adminMarker);
        assertThat(bobList.body()).contains(bobMarker).doesNotContain(aliceMarker, adminMarker);
        assertThat(adminList.body()).contains(adminMarker).doesNotContain(aliceMarker, bobMarker);
        assertThat(array(aliceList)).hasSize(1);
        assertThat(array(bobList)).hasSize(1);
        assertThat(array(adminList)).hasSize(1);

        HttpResponse<String> aliceSummary = request("GET", "/api/transactions/summary", alice.token(), null);
        HttpResponse<String> bobSummary = request("GET", "/api/transactions/summary", bob.token(), null);
        assertStatus(aliceSummary, 200);
        assertStatus(bobSummary, 200);
        assertThat(number(aliceSummary, "$.totalIncome").doubleValue()).isEqualTo(101.25);
        assertThat(number(aliceSummary, "$.totalExpense").doubleValue()).isZero();
        assertThat(number(aliceSummary, "$.transactionCount").intValue()).isEqualTo(1);
        assertThat(number(bobSummary, "$.totalIncome").doubleValue()).isZero();
        assertThat(number(bobSummary, "$.totalExpense").doubleValue()).isEqualTo(31.50);

        assertStatus(request("DELETE", "/api/transactions/" + aliceTransaction, bob.token(), null), 404);
        assertStatus(request("DELETE", "/api/transactions/" + aliceTransaction, administrator.token(), null), 404);
        assertStatus(request("DELETE", "/api/transactions/" + UUID.randomUUID(), bob.token(), null), 404);
        assertThat(request("GET", "/api/transactions", alice.token(), null).body()).contains(aliceTransaction);
        assertStatus(request("DELETE", "/api/transactions/" + bobTransaction, bob.token(), null), 204);
        assertThat(array(request("GET", "/api/transactions", bob.token(), null))).isEmpty();
    }

    @Test
    void assistantTransactionsAndAuditHistoryRemainScopedToTheAuthenticatedPrincipal() throws Exception {
        Principal alice = newPrincipal("USER");
        Principal bob = newPrincipal("USER");
        Principal administrator = newPrincipal("ADMIN");
        String aliceMarker = "privatealice" + UUID.randomUUID();
        String bobMarker = "privatebob" + UUID.randomUUID();
        String aliceCommand = "CONFIRMAR despesa de 27,49 na categoria TESTE " + aliceMarker;

        assertStatus(request("POST", "/api/assistant/text", alice.token(),
                "{\"message\":\"" + aliceCommand + "\"}"), 200);
        assertStatus(request("POST", "/api/assistant/text", bob.token(),
                "{\"message\":\"Qual meu saldo? " + bobMarker + "\"}"), 200);

        HttpResponse<String> aliceAudits = request("GET", "/api/audits", alice.token(), null);
        HttpResponse<String> bobAudits = request("GET", "/api/audits", bob.token(), null);
        HttpResponse<String> adminAudits = request("GET", "/api/audits", administrator.token(), null);
        assertStatus(aliceAudits, 200);
        assertStatus(bobAudits, 200);
        assertStatus(adminAudits, 200);
        assertThat(aliceAudits.body()).contains(aliceMarker).doesNotContain(bobMarker);
        assertThat(bobAudits.body()).contains(bobMarker).doesNotContain(aliceMarker);
        assertThat(array(adminAudits)).isEmpty();

        HttpResponse<String> aliceTransactions = request("GET", "/api/transactions", alice.token(), null);
        assertStatus(aliceTransactions, 200);
        assertThat(array(aliceTransactions)).hasSize(1);
        assertThat(number(request("GET", "/api/transactions/summary", alice.token(), null),
                "$.totalExpense").doubleValue()).isEqualTo(27.49);
        assertThat(array(request("GET", "/api/transactions", bob.token(), null))).isEmpty();

        HttpResponse<String> bobAssistant = request("POST", "/api/assistant/text", bob.token(),
                "{\"message\":\"Liste minhas transações\"}");
        assertStatus(bobAssistant, 200);
        assertThat(bobAssistant.body()).doesNotContain(aliceMarker, "27.49");
    }

    @Test
    void ordinaryUsersCannotCreateListRotateOrRevokePrincipals() throws Exception {
        Principal user = newPrincipal("USER");
        Principal other = newPrincipal("USER");
        assertStatus(request("GET", "/api/admin/principals", user.token(), null), 403);
        assertStatus(request("POST", "/api/admin/principals", user.token(),
                "{\"username\":\"attempted-escalation\",\"role\":\"ADMIN\"}"), 403);
        assertStatus(request("POST", "/api/admin/principals/" + other.id() + "/rotate-token",
                user.token(), null), 403);
        assertStatus(request("POST", "/api/admin/principals/" + other.id() + "/revoke",
                user.token(), null), 403);
        assertStatus(request("GET", "/api/transactions", other.token(), null), 200);
    }

    @Test
    void tokenRotationInvalidatesOldCredentialAndRevocationInvalidatesTheNewOne() throws Exception {
        Principal user = newPrincipal("USER");
        String marker = "retained-after-rotation-" + UUID.randomUUID();
        createTransaction(user.token(), "INCOME", "15.00", marker);

        HttpResponse<String> rotated = request("POST", "/api/admin/principals/" + user.id()
                + "/rotate-token", ADMIN_TOKEN, null);
        assertStatus(rotated, 200);
        assertNoStore(rotated);
        String newToken = string(rotated, "$.token");
        assertThat(newToken).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(user.token());
        assertThat(string(rotated, "$.id")).isEqualTo(user.id());
        assertStatus(request("GET", "/api/transactions", user.token(), null), 401);
        HttpResponse<String> retained = request("GET", "/api/transactions", newToken, null);
        assertStatus(retained, 200);
        assertThat(retained.body()).contains(marker);

        HttpResponse<String> revoked = request("POST", "/api/admin/principals/" + user.id()
                + "/revoke", ADMIN_TOKEN, null);
        assertThat(revoked.statusCode()).isBetween(200, 299);
        assertStatus(request("GET", "/api/transactions", newToken, null), 401);
        assertStatus(request("GET", "/api/audits", newToken, null), 401);
        assertStatus(request("POST", "/api/assistant/text", newToken,
                "{\"message\":\"Qual meu saldo?\"}"), 401);
    }

    @Test
    void principalListingsNeverExposeCredentialsAndAdministratorsCannotRevokeThemselves() throws Exception {
        Principal user = newPrincipal("USER");
        Principal administrator = newPrincipal("ADMIN");
        HttpResponse<String> listed = request("GET", "/api/admin/principals", ADMIN_TOKEN, null);
        assertStatus(listed, 200);
        assertThat(array(listed)).isNotEmpty();
        assertThat(listed.body()).contains(user.username(), administrator.username())
                .doesNotContain(user.token(), administrator.token(), ADMIN_TOKEN,
                        "\"token\"", "tokenHash", "token_hash", "password", "secret");

        HttpResponse<String> selfRevoke = request("POST", "/api/admin/principals/" + administrator.id()
                + "/revoke", administrator.token(), null);
        assertThat(selfRevoke.statusCode()).isBetween(400, 499);
        assertStatus(request("GET", "/api/admin/principals", administrator.token(), null), 200);
    }

    @Test
    void legacyRowsWithoutOwnersRemainQuarantinedForUsersAndAdministrators() throws Exception {
        Principal user = newPrincipal("USER");
        Principal administrator = newPrincipal("ADMIN");
        UUID legacyId = UUID.randomUUID();
        String marker = "legacy-unassigned-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO financial_transactions
                    (id, type, transaction_amount, description, category, occurred_at, created_at)
                VALUES (?, 'INCOME', 9999.00, ?, 'LEGACY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, legacyId, marker);
        jdbc.update("""
                INSERT INTO ai_interactions
                    (channel, mode, input_text, response_text, status, created_at)
                VALUES ('TEXT', 'demo', ?, ?, 'SUCCESS', CURRENT_TIMESTAMP)
                """, marker, marker);

        for (String token : List.of(user.token(), administrator.token(), ADMIN_TOKEN)) {
            HttpResponse<String> transactions = request("GET", "/api/transactions", token, null);
            HttpResponse<String> audits = request("GET", "/api/audits", token, null);
            assertStatus(transactions, 200);
            assertStatus(audits, 200);
            assertThat(transactions.body()).doesNotContain(marker, legacyId.toString());
            assertThat(audits.body()).doesNotContain(marker);
            HttpResponse<String> summary = request("GET", "/api/transactions/summary", token, null);
            assertStatus(summary, 200);
            assertThat(number(summary, "$.transactionCount").intValue()).isZero();
            assertStatus(request("DELETE", "/api/transactions/" + legacyId, token, null), 404);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM financial_transactions WHERE id = ?",
                Integer.class, legacyId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_interactions WHERE input_text = ?",
                Integer.class, marker)).isEqualTo(1);
    }

    private Principal newPrincipal(String role) throws Exception {
        String username = "test-" + UUID.randomUUID();
        HttpResponse<String> response = request("POST", "/api/admin/principals", ADMIN_TOKEN,
                "{\"username\":\"" + username + "\",\"role\":\"" + role + "\"}");
        assertStatus(response, 201);
        assertNoStore(response);
        String token = string(response, "$.token");
        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat((Boolean) JsonPath.read(response.body(), "$.active")).isTrue();
        assertThat(string(response, "$.role")).isEqualTo(role);
        return new Principal(string(response, "$.id"), username, token);
    }

    private String createTransaction(String token, String type, String amount, String description) throws Exception {
        HttpResponse<String> response = request("POST", "/api/transactions", token,
                "{\"type\":\"" + type + "\",\"amount\":" + amount + ",\"description\":\""
                        + description + "\",\"category\":\"TEST\"}");
        assertStatus(response, 201);
        return string(response, "$.id");
    }

    private HttpResponse<String> request(String method, String path, String token, String body) throws Exception {
        return requestWithHeaders(method, path, body,
                token == null ? Map.of() : Map.of("Authorization", "Bearer " + token));
    }

    private HttpResponse<String> requestWithHeaders(String method, String path, String body,
                                                   Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json");
        headers.forEach(builder::header);
        return CLIENT.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void assertStatus(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).as("HTTP status for %s: %s", response.uri(), response.body())
                .isEqualTo(status);
    }

    private static void assertNoStore(HttpResponse<String> response) {
        assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
    }

    private static List<?> array(HttpResponse<String> response) {
        return JsonPath.read(response.body(), "$");
    }

    private static String string(HttpResponse<String> response, String path) {
        return JsonPath.read(response.body(), path);
    }

    private static Number number(HttpResponse<String> response, String path) {
        return JsonPath.read(response.body(), path);
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record Principal(String id, String username, String token) {
    }
}

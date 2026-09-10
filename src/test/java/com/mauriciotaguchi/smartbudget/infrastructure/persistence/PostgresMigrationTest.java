package com.mauriciotaguchi.smartbudget.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "TEST_POSTGRES_URL", matches = ".+")
class PostgresMigrationTest {

    private String url;
    private String username;
    private String password;
    private String schema;
    private Connection connection;

    @BeforeEach
    void createIsolatedLocalTestSchema() throws SQLException {
        url = System.getenv("TEST_POSTGRES_URL");
        if (!url.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/smartbudget_ci")) {
            throw new IllegalArgumentException("Only an explicit local smartbudget_ci test database is allowed");
        }
        username = System.getenv("TEST_POSTGRES_USERNAME");
        password = System.getenv("TEST_POSTGRES_PASSWORD");
        schema = "lgpd_test_" + UUID.randomUUID().toString().replace("-", "");
        connection = DriverManager.getConnection(url, username, password);
        execute("CREATE SCHEMA " + schema);
        connection.setSchema(schema);
    }

    @AfterEach
    void removeOnlyThisTestSchema() throws SQLException {
        if (connection != null) {
            try {
                if (schema != null && schema.matches("lgpd_test_[0-9a-f]{32}")) {
                    execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
                }
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void freshPostgresSchemaMigratesWithoutCreatingAnAnonymousOwner() throws SQLException {
        Flyway flyway = configuration().load();
        flyway.migrate();
        flyway.validate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
        assertThat(count("SELECT COUNT(*) FROM auth_principals")).isZero();
        assertThat(count("SELECT COUNT(*) FROM auth_administration_lock WHERE id = 1")).isOne();
        assertThat(count("SELECT COUNT(*) FROM financial_transactions")).isZero();
        assertThat(count("SELECT COUNT(*) FROM ai_interactions")).isZero();
    }

    @Test
    void legacyPostgresRequiresExplicitBaselineAndPreservesUnassignedRecords() throws Exception {
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(
                "/db/migration/V1__original_financial_schema.sql"))) {
            String legacyDdl = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : legacyDdl.split(";")) {
                if (!statement.isBlank()) {
                    execute(statement);
                }
            }
        }
        UUID transactionId = UUID.randomUUID();
        execute("INSERT INTO financial_transactions "
                + "(id, type, transaction_amount, description, category, occurred_at, created_at) "
                + "VALUES ('" + transactionId + "', 'EXPENSE', 12.34, 'synthetic legacy record', "
                + "'TEST', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        execute("INSERT INTO ai_interactions (channel, mode, input_text, response_text, status, created_at) "
                + "VALUES ('TEXT', 'demo', 'synthetic legacy request', 'synthetic response', 'SUCCESS', CURRENT_TIMESTAMP)");

        assertThatThrownBy(() -> configuration().load().migrate()).isInstanceOf(FlywayException.class);
        assertThat(count("SELECT COUNT(*) FROM financial_transactions WHERE transaction_amount = 12.34")).isOne();

        Flyway reviewedMigration = configuration().baselineVersion("1").load();
        reviewedMigration.baseline();
        reviewedMigration.migrate();
        reviewedMigration.validate();

        assertThat(count("SELECT COUNT(*) FROM financial_transactions WHERE owner_id IS NULL "
                + "AND description = 'synthetic legacy record' AND transaction_amount = 12.34")).isOne();
        assertThat(count("SELECT COUNT(*) FROM ai_interactions WHERE owner_id IS NULL "
                + "AND input_text = 'synthetic legacy request'")).isOne();
        UUID unrelatedOwner = UUID.randomUUID();
        assertThat(count("SELECT COUNT(*) FROM financial_transactions WHERE owner_id = '" + unrelatedOwner + "'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM ai_interactions WHERE owner_id = '" + unrelatedOwner + "'")).isZero();
        assertThatThrownBy(() -> execute("UPDATE financial_transactions SET owner_id = '" + unrelatedOwner + "'"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> execute("UPDATE ai_interactions SET owner_id = '" + unrelatedOwner + "'"))
                .isInstanceOf(SQLException.class);
    }

    private FluentConfiguration configuration() {
        return Flyway.configure()
                .dataSource(url, username, password)
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(false)
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .locations("classpath:db/migration");
    }

    private void execute(String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }
}

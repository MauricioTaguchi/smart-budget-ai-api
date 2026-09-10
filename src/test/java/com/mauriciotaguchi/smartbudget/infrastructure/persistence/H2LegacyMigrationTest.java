package com.mauriciotaguchi.smartbudget.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.Objects;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class H2LegacyMigrationTest {
    @Test
    void nonVersionedLegacyDataRequiresExplicitBaselineAndKeepsNullOwnership() throws Exception {
        String url = "jdbc:h2:mem:legacy_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var source = Objects.requireNonNull(getClass().getResourceAsStream(
                     "/db/migration/V1__original_financial_schema.sql"))) {
            for (String sql : new String(source.readAllBytes(), StandardCharsets.UTF_8).split(";")) {
                if (!sql.isBlank()) statement.execute(sql);
            }
            statement.execute("INSERT INTO financial_transactions VALUES (RANDOM_UUID(), 'EXPENSE', 7.50,"
                    + " 'legacy synthetic', 'TEST', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
            statement.execute("INSERT INTO ai_interactions "
                    + "(channel,mode,input_text,response_text,status,created_at) VALUES "
                    + "('TEXT','demo','legacy input','legacy output','SUCCESS',CURRENT_TIMESTAMP)");

            var migration = Flyway.configure().dataSource(url, "sa", "").baselineOnMigrate(false)
                    .baselineVersion("1").cleanDisabled(true).load();
            assertThatThrownBy(migration::migrate).isInstanceOf(FlywayException.class);
            migration.baseline();
            migration.migrate();
            migration.validate();

            try (var rows = statement.executeQuery("SELECT description, owner_id FROM financial_transactions")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("legacy synthetic");
                assertThat(rows.getObject(2)).isNull();
                assertThat(rows.next()).isFalse();
            }
            try (var rows = statement.executeQuery("SELECT input_text, owner_id FROM ai_interactions")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("legacy input");
                assertThat(rows.getObject(2)).isNull();
                assertThat(rows.next()).isFalse();
            }
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM auth_principals")) {
                rows.next();
                assertThat(rows.getLong(1)).isZero();
            }
        }
    }
}


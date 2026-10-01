package io.github.caioeduardopereirafelix.financeapi.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LowercaseEmailsMigrationPostgresTest {

    private static final String EXTERNAL_URL = System.getenv("TEST_PG_URL");

    @Test
    void migrationMinusculizaOsEmailsMasNaoMexeNosQueEntrariamEmConflito() throws Exception {
        assumeTrue(EXTERNAL_URL != null || DockerClientFactory.instance().isDockerAvailable(),
                "Sem Docker e sem TEST_PG_URL: teste contra PostgreSQL pulado");

        PostgreSQLContainer<?> container = null;
        String url = EXTERNAL_URL;
        String user = System.getenv().getOrDefault("TEST_PG_USER", "postgres");
        String password = System.getenv().getOrDefault("TEST_PG_PASSWORD", "");
        if (url == null) {
            container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
            container.start();
            url = container.getJdbcUrl();
            user = container.getUsername();
            password = container.getPassword();
        }

        String schema = "mig10_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway before = Flyway.configure().dataSource(url, user, password)
                    .schemas(schema).target("9").load();
            before.migrate();

            try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
                s.execute("set search_path to " + schema);
                insert(s, "Misto@Exemplo.COM");
                insert(s, "ja-minusculo@exemplo.com");
                insert(s, "  Espacos@Exemplo.com ");
                insert(s, "Conflito@Exemplo.com");
                insert(s, "conflito@exemplo.com");
            }

            Flyway.configure().dataSource(url, user, password).schemas(schema).load().migrate();

            Map<String, Integer> found = new HashMap<>();
            try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
                s.execute("set search_path to " + schema);
                ResultSet rs = s.executeQuery("select email from users");
                while (rs.next()) {
                    found.merge(rs.getString(1), 1, Integer::sum);
                }
            }

            assertEquals(1, found.get("misto@exemplo.com"));
            assertEquals(1, found.get("ja-minusculo@exemplo.com"));
            assertEquals(1, found.get("espacos@exemplo.com"));
            assertEquals(1, found.get("Conflito@Exemplo.com"));
            assertEquals(1, found.get("conflito@exemplo.com"));
            assertEquals(5, found.values().stream().mapToInt(Integer::intValue).sum());
        } finally {
            try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
                s.execute("drop schema if exists " + schema + " cascade");
            }
            if (container != null) {
                container.stop();
            }
        }
    }

    private static void insert(Statement s, String email) throws Exception {
        s.execute("insert into users (id, first_name, email, password) values ('" + UUID.randomUUID()
                + "', 'T', '" + email + "', 'x')");
    }
}

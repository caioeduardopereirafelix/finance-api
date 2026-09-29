package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Base dos testes que rodam contra um PostgreSQL de verdade, com as migrations do Flyway e
 * {@code ddl-auto=validate}, exatamente como em producao. Os demais testes usam H2, que aceita
 * o SQL mas nao prova que o schema real esta certo.
 *
 * <p>O banco vem de um container (Testcontainers, precisa de Docker). Sem Docker os testes sao
 * pulados, nao falham. Para usar um PostgreSQL ja existente (por exemplo, no CI), defina
 * {@code TEST_PG_URL}, {@code TEST_PG_USER} e {@code TEST_PG_PASSWORD}: use um banco vazio e
 * descartavel, porque o Flyway vai criar as tabelas nele.
 */
abstract class PostgresIntegrationTestSupport extends ApiIntegrationTestSupport {

    private static final String EXTERNAL_URL = System.getenv("TEST_PG_URL");

    private static PostgreSQLContainer<?> container;

    @BeforeAll
    static void requirePostgres() {
        assumeTrue(EXTERNAL_URL != null || DockerClientFactory.instance().isDockerAvailable(),
                "Sem Docker e sem TEST_PG_URL: testes contra PostgreSQL pulados");
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresIntegrationTestSupport::url);
        registry.add("spring.datasource.username", PostgresIntegrationTestSupport::user);
        registry.add("spring.datasource.password", PostgresIntegrationTestSupport::password);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("bank.mock.enabled", () -> "true");
    }

    private static String url() {
        return EXTERNAL_URL != null ? EXTERNAL_URL : container().getJdbcUrl();
    }

    private static String user() {
        return EXTERNAL_URL != null ? env("TEST_PG_USER", "postgres") : container().getUsername();
    }

    private static String password() {
        return EXTERNAL_URL != null ? env("TEST_PG_PASSWORD", "") : container().getPassword();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value != null ? value : fallback;
    }

    private static synchronized PostgreSQLContainer<?> container() {
        if (container == null) {
            container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
            container.start();
        }
        return container;
    }
}

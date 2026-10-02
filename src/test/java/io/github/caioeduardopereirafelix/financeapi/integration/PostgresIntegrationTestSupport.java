package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

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

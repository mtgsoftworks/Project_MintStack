package com.mintstack.finance.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests using Testcontainers.
 * Uses PostgreSQL/Redis containers when Docker is available,
 * and falls back to H2 test profile properties when it is not.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final boolean DOCKER_AVAILABLE = isDockerAvailable();

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = DOCKER_AVAILABLE
            ? new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("mintstack_test")
                    .withUsername("test")
                    .withPassword("test")
            : null;

    @SuppressWarnings("resource")
    static final GenericContainer<?> redis = DOCKER_AVAILABLE
            ? new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
            : null;

    static {
        if (DOCKER_AVAILABLE) {
            postgres.start();
            redis.start();
        }
    }

    private static boolean isDockerAvailable() {
        if ("true".equalsIgnoreCase(System.getenv("CI")) || "true".equalsIgnoreCase(System.getenv("TESTCONTAINERS_DISABLED"))) {
            return false;
        }
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (DOCKER_AVAILABLE) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);

            registry.add("spring.data.redis.host", redis::getHost);
            registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
            registry.add("spring.data.redis.password", () -> "");
            registry.add("spring.flyway.enabled", () -> "true");
            registry.add("app.redis.cache.enabled", () -> "true");
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.flyway.enabled", () -> "false");
            registry.add("spring.data.redis.repositories.enabled", () -> "false");
            registry.add("spring.cache.type", () -> "none");
            registry.add("app.redis.cache.enabled", () -> "false");
        }

        // Disable Kafka and async messaging for integration tests
        registry.add("app.messaging.enabled", () -> "false");

        // Disable external API calls
        registry.add("app.scheduler.enabled", () -> "false");
    }
}

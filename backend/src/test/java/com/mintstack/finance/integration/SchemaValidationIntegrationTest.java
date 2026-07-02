package com.mintstack.finance.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "app.messaging.enabled=false",
                "app.scheduler.enabled=false",
                "app.redis.cache.enabled=false"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SchemaValidationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("mintstack_schema_validation")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Test
    void flywaySchemaMatchesJpaMappings() {
        // Context startup performs Flyway migration followed by Hibernate schema validation.
    }
}

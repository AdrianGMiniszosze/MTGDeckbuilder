package com.deckbuilder.mtgdeckbuilder.integration;

import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against entity/schema drift. Flyway builds the schema from scratch on
 * an empty database and Hibernate validates the entities against it instead of
 * creating or updating tables. A mismatch fails the context startup.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@ActiveProfiles("testcontainers")
class SchemaValidationIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("mtg_validate")
            .withUsername("test")
            .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private CardRepository cardRepository;

    @Test
    void validatesEntitiesAgainstFlywaySchema() {
        assertThat(this.cardRepository.count()).isZero();
    }
}

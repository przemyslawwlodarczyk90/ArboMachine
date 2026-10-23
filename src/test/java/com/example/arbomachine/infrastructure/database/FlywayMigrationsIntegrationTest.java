package com.example.arbomachine.infrastructure.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Etap 38: migracje z db/migration na pustej bazie daja schemat zgodny z encjami.
 * ddl-auto=validate - kontekst nie wstanie, jesli migracjom brakuje tabeli lub kolumny z encji.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flywaymigrationstest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=true",
        "spring.flyway.schemas=arbomachine",
        "spring.flyway.default-schema=arbomachine",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=arbomachine",
        "auth.jwt.secret=flyway-migrations-test-secret"
})
class FlywayMigrationsIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void shouldApplyAllMigrationsAndMatchEntities() {
        // applied() zawiera tez wpis Flyway o zalozeniu schematu - liczy sie numer ostatniej wersji.
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("6");
        assertThat(flyway.info().pending()).isEmpty();
    }
}

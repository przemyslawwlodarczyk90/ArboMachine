# Spring Boot 4 i przygotowanie do wspólnego hostingu AWS — 01.11.2026

Zakres: ArboMachine przygotowany do wspólnego serwera AWS z ArnoCRM (EC2 t3.small 2 GiB, EBS gp3 30 GiB,
jeden PostgreSQL 18.6 z osobnymi bazami, Caddy z HTTPS). **Bez wdrożenia** — wdrożenie wymaga osobnej zgody.
Instrukcja budowania i wdrożenia: [`deploy/README.md`](../deploy/README.md).

## Etap A — audyt przed zmianami (stan wyjściowy, commit `1ebc68e`)

| Obszar | Stan przed | Uwagi |
|---|---|---|
| Java | 25 (build `maven:3.9-eclipse-temurin-25`, runtime `eclipse-temurin:25-jre`) | bez zmian |
| Spring Boot | 3.5.16 (Spring 6.2, Security 6.5, Hibernate 6.6, Jackson 2.21, Tomcat 10.1) | |
| PostgreSQL | 14 (`postgres:14-alpine`), schemat `arbomachine`, sterownik 42.7.11 | |
| Schemat | `ddl-auto=update`; Flyway V1–V10 tylko w testach (H2) | |
| Testy | 407, 0 błędów (wszystkie na H2) | |
| Wdrożenie | `AWS/` (nieśledzony przez Git): własny PostgreSQL w compose, HTTP :80, brak healthchecku aplikacji | |

### Zgodność ze Spring Boot 4.0.9 — co wymagało zmian

| Element | Boot 3.5.16 → 4.0.9 | Wpływ na ArboMachine | Zrobione |
|---|---|---|---|
| Startery | `spring-boot-starter-web` → `spring-boot-starter-webmvc`; Flyway wymaga `spring-boot-starter-flyway` (sam `flyway-core` nie ma już autokonfiguracji) | pom | tak |
| Startery testowe | moduły testowe rozbite: `spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-security-test` | pom | tak |
| Pakiety testowe | `AutoConfigureMockMvc` → `org.springframework.boot.webmvc.test.autoconfigure`; `DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure`; `@MockBean` usunięty → `@MockitoBean` | 27 + 2 + 3 pliki testów | tak |
| Spring Framework 7 / MockMvc | `MockMultipartHttpServletRequestBuilder` nie dziedziczy po `MockHttpServletRequestBuilder` | pomocnicze `performAs(...)` w testach → `AbstractMockHttpServletRequestBuilder<?>` | tak |
| Jackson 3 (`tools.jackson`) | nowe pakiety, `JsonProcessingException` → `JacksonException` (**niekontrolowany**), `JsonNode.fields()` → `properties()`; adnotacje `com.fasterxml.jackson.annotation` bez zmian | ~20 plików main, ~27 testów. **Pułapka:** czytniki GeoJSON łapały `IOException` — w Jackson 3 błędny JSON dawałby 500 zamiast czytelnego błędu importu; poprawione na `JacksonException` | tak |
| Domyślne ustawienia Jackson 3 | inne niż w Jackson 2 (m.in. sortowanie pól, liczby, null dla prymitywów) | `spring.jackson.use-jackson2-defaults=true` — API JSON bez zmian dla frontendu | tak |
| Spring Security 7 | DSL lambdowy bez zmian dla naszej konfiguracji (`SecurityConfig`) | brak zmian w kodzie | — |
| Hibernate 7.2 | `hbm2ddl.create_namespaces` niepotrzebne (schemat tworzy Flyway); testy H2: `jakarta.persistence.create-database-schemas=true` | konfiguracja | tak |
| springdoc | 2.8.x → 3.0.3 (linia dla Boot 4) | pom; w profilu prod wyłączony | tak |
| Walidacja, Thymeleaf, Mail, MVC (`WebMvcConfigurer`, `PathResourceResolver` SPA) | bez zmian API | sprawdzone testami | — |
| Lombok | 1.18.48 (z Boot 4 BOM taka sama) | — | — |
| Biblioteki spoza Spring (openhtmltopdf 1.1.87, PDFBox 3.0.7, fastexcel 0.20.2, NGA GeoPackage 6.6.5, commons-csv, java-jwt 4.2.1, jsoup) | niezależne od Boot | bez zmian wersji | — |

### Wersje po migracji (z `spring-boot-dependencies-4.0.9`)

Spring Framework 7.0.10, Spring Security 7.0.8, Hibernate ORM 7.2.25.Final, HikariCP 7.0.2, Jackson 3.1.7
(+ adnotacje 2.21), Tomcat 11.0.27, Flyway 11.14.1, sterownik PostgreSQL 42.7.14, Thymeleaf 3.1.5,
Testcontainers 2.0.5, H2 2.4.240 (testy), springdoc 3.0.3.

### Flyway a encje — czy schemat da się zbudować od zera

- DDL wygenerowany z encji (Hibernate 7, dialekt PostgreSQL) porównany z V1–V10: **18/18 tabel, 34/34 nazwanych
  indeksów i ograniczeń** — brak różnic; nowe migracje nie były potrzebne, istniejących nie zmieniano.
- `PostgreSqlDeploymentIntegrationTest` na `postgres:18.6-alpine`: czysta baza → Flyway V1–V10 →
  Hibernate `validate` (tabele, kolumny, typy) — przechodzi. Flyway 11.14.1 rozpoznaje „PostgreSQL 18.6” bez ostrzeżeń.
- Migracje mają nazwę schematu wpisaną na stałe (`arbomachine.xxx`) — dlatego `DB_SCHEMA` musi być `arbomachine`.

## Etapy B–E — co zmieniono

| Etap | Zmiana |
|---|---|
| B | Spring Boot 4.0.9, Jackson 3, startery i testy jak wyżej. Java 25 bez zmian. |
| C | Flyway w runtime, `ddl-auto=validate`, `open-in-view=false`; konfiguracja `DB_HOST/DB_PORT/DB_NAME/DB_SCHEMA/DB_USERNAME/DB_PASSWORD` (opcjonalnie `SPRING_DATASOURCE_URL`); Hikari 4/1 (`DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE`); skrypt bazy `deploy/postgres/initdb/10-arbomachine.sh`; test na PostgreSQL 18.6 z izolacją od bazy `arnocrm`. |
| D | `SMTP_HOST/SMTP_PORT/SMTP_USERNAME/SMTP_APP_PASSWORD`, STARTTLS wymagany; `JWT_SECRET` (brak = aplikacja nie startuje); `APP_PUBLIC_BASE_URL`; profil `prod`: Swagger wyłączony, `forward-headers-strategy=framework`, cache szablonów; actuator tylko `/actuator/health` bez szczegółów. |
| E | `deploy/Dockerfile`, `deploy/build-image.sh` (tag `<wersja>-<commit>`), fragment Compose, Caddy, `.env.example`, lokalny test odbiorczy. Limit 1 generowania PDF naraz (503 + Retry-After po 120 s czekania). |

## Lokalne środowisko deweloperskie (IntelliJ)

Domyślne wartości nadal wskazują lokalną bazę `pgDB` (`localhost:5432`, schemat `arbomachine`). Przy pierwszym starcie
po tej zmianie Flyway zastanie schemat zbudowany kiedyś przez `ddl-auto=update` bez historii migracji — oznaczy go jako
V1 (`baseline-on-migrate`, domyślnie włączone poza profilem prod) i wykona V2–V10, które są idempotentne
(`if not exists`). Dane zostają. Zalecana kopia przed pierwszym startem: `pg_dump -n arbomachine`.

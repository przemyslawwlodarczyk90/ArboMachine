# Migracje bazy danych (Flyway)

**Stan od 01.11.2026 (Spring Boot 4, przygotowanie AWS):** schemat tworzy i aktualizuje **wyłącznie Flyway**
(`spring-boot-starter-flyway` w runtime), Hibernate ma `ddl-auto=validate`. Migracje V1–V10 budują pełny schemat od zera
(sprawdzone na PostgreSQL 18.6 — `PostgreSqlDeploymentIntegrationTest`; encje vs migracje: 18/18 tabel, 34/34 indeksów
i ograniczeń). Lokalna baza zbudowana kiedyś przez `ddl-auto=update` dostaje przy pierwszym starcie baseline V1
(`FLYWAY_BASELINE_ON_MIGRATE`, poza profilem prod domyślnie `true`), potem V2–V10. Szczegóły:
`docs/MIGRACJA_SPRING_BOOT_4_I_AWS.md`. Poniżej — historia z etapu 38 (przełączenie opisane w ostatniej sekcji zostało
wykonane 01.11.2026, z tą różnicą, że produkcja startuje z czystą bazą).

## Pliki

| Plik | Zawartość |
|---|---|
| `src/main/resources/db/migration/V1__schemat_przed_etapem_33.sql` | Pełny schemat `arbomachine` sprzed etapu 33 (17 tabel), wygenerowany z encji dialektem PostgreSQL. |
| `V2__etap_33_zdjecia_offline_client_upload_id.sql` | `tree_photo.client_upload_id` + unikalność (drzewo, identyfikator uploadu). |
| `V3__etap_35_dane_pomiaru_lokalizacji.sql` | `tree.location_measured_at`, `location_adapter_code`, `location_device_label`. |
| `V4__etap_36_client_request_id.sql` | `client_request_id` w `tree`, `tree_work`, `tree_monitoring_record` + unikalność. |
| `V5__etap_37_wersja_danych_drzewa.sql` | `tree.data_version bigint default 0 not null`. |

V2–V5 używają `add column if not exists` / `create unique index if not exists` — przechodzą także na bazie, w której
`ddl-auto=update` już dodał te kolumny (stan produkcji po wdrożeniu etapów 33–37).

## Jak to jest sprawdzane

- `FlywayMigrationsIntegrationTest` — pusta baza H2 (tryb PostgreSQL), Flyway wykonuje V1–V5, Hibernate z
  `ddl-auto=validate` sprawdza, że każda tabela i kolumna encji istnieje z właściwym typem. Nowa kolumna w encji bez
  migracji = czerwony test.
- Pozostałe testy budują schemat jak dotąd przez Hibernate (`src/test/resources/config/application.properties`:
  `spring.flyway.enabled=false`).
- Ręcznie 22.10.2026 na lokalnym PostgreSQL (schemat tymczasowy, potem usunięty): V1–V5 bez błędów, ponowne V2–V5 bez
  błędów, kolumny (nazwa, typ, długość, NULL, domyślna wartość) identyczne ze schematem zbudowanym przez
  `ddl-auto=update` — 148/148. Różnice tylko w indeksach: nazwy automatycznych ograniczeń unikalności (Hibernate
  `uk…` zamiast `…_key` PostgreSQL) i kolejność kolumn klucza głównego `tree_stand_tree`. Nie wpływają na działanie.

## Zasada na kolejne etapy

Każda zmiana encji = nowy plik `V<n>__etap_<nr>_<opis>.sql` w tym samym commicie (test przypomni). Pisać migracje tak,
żeby przechodziły także po `ddl-auto=update` (`if not exists`), dopóki produkcja nie zostanie przełączona.

## Przełączenie produkcji — decyzja właściciela (niewykonane)

Kroki, gdy zapadnie decyzja (wymaga okna serwisowego i kopii bazy):

1. Kopia zapasowa bazy produkcyjnej (`pg_dump`).
2. `pom.xml`: usunąć `<scope>test</scope>` przy `flyway-core` i `flyway-database-postgresql`.
3. Konfiguracja produkcji:
   `spring.flyway.enabled=true`, `spring.flyway.schemas=arbomachine`, `spring.flyway.default-schema=arbomachine`,
   `spring.flyway.baseline-on-migrate=true`, `spring.flyway.baseline-version=1`.
   Flyway oznaczy istniejący schemat jako V1 (bez wykonywania), potem wykona V2–V5 (bez zmian, kolumny już są).
4. Po udanym starcie: `spring.jpa.hibernate.ddl-auto=validate` (zamiast `update`).
5. Środowisko deweloperskie — te same ustawienia; nowa, pusta baza dostaje schemat z V1–V5.

Ryzyko przy zostawieniu `update`: zmiana typu kolumny, PostGIS albo przeniesienie danych nie wykonają się same
(`docs/DO_DOPRECYZOWANIA.md`, sekcja „Migracje bazy danych”).

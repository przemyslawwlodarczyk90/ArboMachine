# Wdrożenie ArboMachine na wspólny serwer AWS (ArboMachine + ArnoCRM)

Serwer docelowy: EC2 t3.small (2 GiB RAM), EBS gp3 30 GiB, Docker Compose; wspólne: PostgreSQL 18.6 (osobne bazy
i role), Redis 7 (tylko ArnoCRM), Caddy (HTTPS 80/443). Porty 5432, 6379, 8083 nie są publiczne.
**Ten katalog przygotowuje wdrożenie — samo wdrożenie wymaga osobnej zgody właściciela.**

| Plik | Do czego |
|---|---|
| `Dockerfile`, `Dockerfile.dockerignore` | obraz `linux/amd64`: frontend + JAR (Spring Boot 4.0.9), runtime JRE 25, użytkownik nie-root |
| `build-image.sh` | testy + obraz z tagiem `arbomachine:<wersja>-<commit>` + archiwum `.tar.gz` |
| `compose/arbomachine.service.yml` | serwis do wklejenia we wspólny `docker-compose.yml` |
| `.env.example` | wzór `arbomachine.env` (bez sekretów) |
| `postgres/initdb/10-arbomachine.sh` | rola i baza `arbomachine` na wspólnym PostgreSQL |
| `caddy/arbomachine.Caddyfile` | fragment wspólnego Caddyfile |
| `local-test/compose.yml` | lokalny test odbiorczy (PostgreSQL 18.6 + Mailpit) |

## 1. Budowanie (lokalnie, nigdy na t3.small)

```bash
# Git Bash, Docker Desktop, JAVA_HOME = JDK 25
bash deploy/build-image.sh
# OUTPUT_DIR=... zmienia katalog archiwum (domyślnie deploy/out, poza Gitem)
```

Skrypt: `./mvnw verify` (wszystkie testy, w tym test na PostgreSQL 18.6 w Dockerze) → `docker build --platform
linux/amd64` → `docker save | gzip`. Tag z przyrostkiem `-dirty` = obraz z niezacommitowanych zmian (nie wdrażać).

## 2. Zmienne środowiskowe

Wzór: `deploy/.env.example`. Na serwerze plik `arbomachine.env` obok wspólnego compose, `chmod 600`, poza Gitem.

| Zmienna | Opis |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_SCHEMA`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL (`postgres`, `5432`, `arbomachine`, `arbomachine`, `arbomachine`, hasło roli). `SPRING_DATASOURCE_URL` nadpisuje cały adres. `DB_SCHEMA` musi być `arbomachine` (wpisany w migracje). |
| `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE` | pula połączeń (domyślnie 4 / 1) |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_APP_PASSWORD` | Gmail `smtp.gmail.com:587`, STARTTLS wymagany (`SMTP_STARTTLS_REQUIRED=true` domyślnie) |
| `JWT_SECRET` | min. 32 losowe znaki; brak = aplikacja nie startuje |
| `APP_PUBLIC_BASE_URL` | `https://<domena>` — linki w mailach (potwierdzenie konta, reset hasła) |
| `APP_REGISTRATION_EMAIL_CONFIRMATION_REQUIRED` | `true` (domyślnie w prod) |
| `DEMO_ENABLED` | `true` = tryb demo na stronie logowania (konto demo + 4 projekty pokazowe, tylko do odczytu – zapis z konta demo zawsze 403). Domyślnie `false`. Opcjonalnie `DEMO_SESSION_HOURS` (4), `DEMO_ACCOUNT_EMAIL`. |
| `REGISTRATION_OFFICIAL_ENABLED` | `false` (domyślnie) – rejestracja konta urzędnika zamknięta |
| `APP_PUSH_VAPID_PUBLIC_KEY`, `APP_PUSH_VAPID_PRIVATE_KEY`, `APP_PUSH_VAPID_SUBJECT` | stałe klucze Web Push (bez nich klucze generują się i lądują w bazie) |
| `PDF_MAX_CONCURRENT_GENERATIONS`, `PDF_MAX_WAIT_SECONDS` | 1 / 120 — ochrona przed OOM |
| `FLYWAY_BASELINE_ON_MIGRATE` | `false` w prod (czysta baza) |

`SPRING_PROFILES_ACTIVE=prod` jest ustawiony w obrazie.

## 3. Baza na wspólnym PostgreSQL

Bazę zakłada infrastruktura, tabele — Flyway przy starcie aplikacji.

- Nowy wolumen PostgreSQL: zamontować `postgres/initdb/10-arbomachine.sh` w `/docker-entrypoint-initdb.d/`
  i przekazać kontenerowi `ARBOMACHINE_DB_PASSWORD`.
- Działający serwer: `docker exec -i -e ARBOMACHINE_DB_PASSWORD=... postgres sh < deploy/postgres/initdb/10-arbomachine.sh`
  (skrypt jest idempotentny).

Rola `arbomachine`: bez superusera, właściciel tylko bazy `arbomachine`, limit 10 połączeń, `CONNECT` odebrany `PUBLIC`.

## 4. Checklista wdrożenia (do wykonania po zgodzie)

1. [ ] `bash deploy/build-image.sh` — zielone testy, tag bez `-dirty`; zanotować tag.
2. [ ] Wysłać archiwum na serwer (`scp`), `gunzip -c arbomachine-<tag>.tar.gz | docker load`.
3. [ ] Wspólny PostgreSQL 18.6 działa; baza `arbomachine` założona skryptem (pkt 3); `psql -U arbomachine -d arnocrm` → odmowa.
4. [ ] `arbomachine.env` z `.env.example`, `chmod 600`; sekrety tylko tam.
5. [ ] Wkleić `compose/arbomachine.service.yml` do wspólnego compose; `ARBOMACHINE_IMAGE_TAG=<tag>`.
6. [ ] Caddy: wkleić `caddy/arbomachine.Caddyfile` (domena), DNS na Elastic IP; security group: tylko 22 (twoje IP), 80, 443.
7. [ ] `docker compose up -d arbomachine`; `docker compose ps` → `healthy` (start do ~2 min: Flyway + Spring).
8. [ ] Rejestracja → mail z linkiem (`APP_PUBLIC_BASE_URL`) → logowanie → projekt, drzewo, zdjęcie, PDF.
9. [ ] `docker compose restart arbomachine` → zdjęcie nadal widoczne.
10. [ ] `docker stats` — ArboMachine w spoczynku ~300 MiB; po PDF do ~700 MiB (limit 768).
11. [ ] Kopia zapasowa: `pg_dump -Fc arbomachine` + wolumen `arbomachine-tree-photos` (razem, w tym samym oknie).

## 5. Wycofanie wersji (rollback)

Obraz każdej wersji ma własny tag. Rollback = `ARBOMACHINE_IMAGE_TAG=<poprzedni tag>` i `docker compose up -d arbomachine`.
**Uwaga:** jeśli nowa wersja wykonała nowe migracje Flyway, stara wersja może nie przejść `validate` — przed wdrożeniem
z nowymi migracjami zrobić `pg_dump`, a rollback robić razem z odtworzeniem bazy. Nigdy `docker compose down -v`
(kasuje wolumeny ze zdjęciami).

## 6. Zasoby na t3.small (2 GiB) — ryzyka

| Składnik | Pamięć |
|---|---|
| ArboMachine | ~300 MiB spoczynek, do ~680 MiB przy PDF ze zdjęciami (limit 768 MiB) |
| ArnoCRM | 450–512 MiB (z kontraktu) |
| PostgreSQL 18.6 (2 bazy) | ~192–256 MiB |
| Redis 7, Caddy | ~30–60 MiB |
| System + Docker | ~250–300 MiB |
| **Razem w szczycie** | **~1,8–1,9 GiB z 2 GiB** — bez zapasu; zalecany swap 1–2 GiB na hoście |

- Po pierwszym PDF pamięć ArboMachine nie wraca do spoczynku (sterta JVM nie jest oddawana) — do restartu kontenera.
- Jeden PDF naraz (kolejka, potem 503). Pojedynczy PDF z wieloma zdjęciami 48 Mpx nadal może zbliżyć się do limitu —
  pełne rozwiązanie: dekodowanie zdjęć w zmniejszonej rozdzielczości (`ReportImageScaler`, DO_DOPRECYZOWANIA §59).
- Kredyty CPU t3: start (~30–60 s) i PDF (~10–20 s na dokument) zużywają kredyty; jednoczesny start obu aplikacji
  po restarcie serwera trwa dłużej.

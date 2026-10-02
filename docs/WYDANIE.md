# Wydanie i uruchomienie ArboMachine

## Wymagania

- JDK 25 (build i uruchomienie), Node.js 20+ (tylko build frontendu), PostgreSQL 14+.
- Produkcja: **HTTPS** (reverse proxy, np. nginx / Caddy) — wymagany przez aparat, GPS, service worker i Web Push.

## Build

```bash
powershell -File scripts/build-release.ps1     # Windows
bash scripts/build-release.sh                  # Linux / macOS / Git Bash
```

Skrypt: `npm ci` (jeśli brak `node_modules`), testy frontendu, build frontendu do `src/main/resources/static`,
`mvnw clean verify` (wszystkie testy backendu), wypisuje ścieżkę JAR i sumę SHA-256. `clean` jest konieczny —
inaczej stare paczki frontendu z `target/classes/static` trafiają do JAR.

## Uruchomienie

```bash
ARBOMACHINE_PORT=8080 \
ARBOMACHINE_DB_URL=jdbc:postgresql://db:5432/arbomachine \
ARBOMACHINE_DB_USER=arbomachine ARBOMACHINE_DB_PASSWORD=... \
ARBOMACHINE_FRONTEND_BASE_URL=https://arbomachine.example.pl \
java -jar target/arbomachine-<wersja>.jar
```

Sekrety (`auth.jwt.secret`, `spring.mail.username`, `spring.mail.password`) — zmienne środowiskowe
`AUTH_JWT_SECRET`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` (plik `arbomachine-secrets.properties` jest czytany
tylko z classpath, więc w produkcji wygodniejsze są zmienne). Bez SMTP: `APP_REGISTRATION_EMAILCONFIRMATIONREQUIRED=false`.
Schemat bazy tworzy i aktualizuje Hibernate przy starcie.

### Web Push

- Wygeneruj stałe klucze VAPID (np. `npx web-push generate-vapid-keys`) i ustaw `app.push.vapid-public-key`,
  `app.push.vapid-private-key` (base64url). Bez nich aplikacja wygeneruje klucze i zapisze je w bazie
  (tabela `web_push_server_key`) — działa, ale klucz prywatny leży wtedy w bazie.
- Ustaw `app.push.vapid-subject=mailto:<prawdziwy adres administratora>`.
- **Zmiana kluczy unieważnia wszystkie subskrypcje** — użytkownicy muszą włączyć powiadomienia ponownie.
- Przypomnienia o pracach: `app.notifications.work-reminders.cron` (domyślnie `0 0 7 * * *`, strefa Europe/Warsaw),
  wyłączenie: `app.notifications.work-reminders.enabled=false`. Przy kilku instancjach aplikacji włącz przypomnienia
  tylko w jednej.

## Dane do pokazu

```bash
node scripts/demo/seed-demo-project.mjs --base https://... --email arborysta@example.pl --password ... \
     [--sheet inwentaryzacja.xlsx] [--drawing rysunek.dxf]
```

Konto musi istnieć. Bez `--sheet`: 8 przykładowych drzew i grupa „Aleja lipowa”. Zawsze: waloryzacja, prace
(jutro, za 5 dni, wykonana), kontrole (jedna „Zły” — wywoła powiadomienie).

## Procedura wydania

1. `git status` czysty, testy zielone (`scripts/build-release.*`).
2. Wpis w README / rejestrze braków, jeśli wydanie zmienia zakres.
3. Tag: `git tag -a v0.30.0 -m "MVP po etapie 30"` i `git push origin v0.30.0` (wersję w `pom.xml` podnieść w tym
   samym commicie, jeśli zmieniana).
4. Zachowaj JAR i jego SHA-256 (artefakt wydania).
5. **Przed wdrożeniem kopia zapasowa** bazy i katalogu zdjęć (niżej).
6. Zatrzymaj starą wersję, uruchom nowy JAR, sprawdź stronę główną, Swagger (`/swagger-ui.html`), logowanie, listę projektów, mapę, PDF.

## Kopie zapasowe

- Baza: `pg_dump -Fc -n arbomachine <baza> > arbomachine-$(date +%F).dump`.
- Zdjęcia: katalog `ARBOMACHINE_TREE_PHOTOS_DIRECTORY` (domyślnie `storage/tree-photos`) — kopia plików razem z bazą
  (rekordy w bazie wskazują pliki na dysku).

## Wycofanie wersji

1. Zatrzymaj aplikację, uruchom poprzedni JAR.
2. Hibernate tylko **dodaje** kolumny i tabele — poprzednia wersja zwykle działa na nowszym schemacie (nowe kolumny
   są opcjonalne i ignorowane).
3. Gdy nie działa: przywróć kopię bazy (`pg_restore --clean -n arbomachine ...`) i katalogu zdjęć sprzed wdrożenia
   (dane wprowadzone po wdrożeniu zostaną utracone).

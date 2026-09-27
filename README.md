# 🌳 ArboMachine

Backend Spring Boot 3.2.5 (Java 17) z pełną obsługą kont użytkowników oraz frontend React (Vite, TypeScript)
w katalogu [`frontend/`](frontend).

Konwencja w kodzie: **nazwy klas, encji, metod i zmiennych po angielsku** i możliwie samoopisujące się
(np. `UserAccount`, `AdminUserAccountManagementService.confirmEmailOnUsersBehalf`), **komentarze
i dokumentacja po polsku**. Interfejs użytkownika i komunikaty API są po angielsku.

---

## 👥 Typy kont

| Rola | Jak się ją dostaje | Panel |
|---|---|---|
| **Arborysta** (`ARBORIST`) | wybór przy rejestracji | `/arborist` |
| **Urzędnik** (`OFFICIAL`) | wybór przy rejestracji | `/official` |
| **Administrator** (`ADMIN`) | **tylko ręcznie w bazie** | `/admin` |

Po zalogowaniu każdy trafia do swojego panelu. Arborysta i urzędnik widzą i zmieniają wyłącznie
**własne** konto. Cudzymi kontami zarządza tylko administrator: edycja danych, zmiana roli
arborysta ↔ urzędnik, blokada/odblokowanie, potwierdzenie e-maila w imieniu użytkownika, usunięcie.
Kont administratorów nie da się zmieniać z panelu admina.

Nadanie roli administratora istniejącemu kontu:

```sql
UPDATE arbomachine.user_account SET role = 'ADMIN' WHERE email = 'ktos@example.com';
```

### Statusy konta

- `emailConfirmed` - użytkownik kliknął link aktywacyjny (albo admin potwierdził e-mail za niego).
  Bez tego nie da się zalogować.
- `lockedByAdmin` - blokada nałożona przez admina. Zablokowane konto nie może się zalogować,
  zarejestrować ponownie ani zresetować hasła, a wydane wcześniej tokeny przestają działać od razu.

---

## 🚀 Funkcje

- 📝 **Rejestracja** z wyborem typu konta i potwierdzeniem e-mail (link ważny 24 h; ponowna rejestracja
  na niepotwierdzony adres wysyła nowy link)
- 🔐 **Logowanie JWT**
- 🔁 **Reset hasła** - jednorazowy link mailem, ważny 30 min
- 🧭 **Trzy panele ról** z osobnym prefiksem ścieżek backendu dla każdej roli
- 🛠️ **Administracja** - statystyki, lista kont z filtrami, akcje na kontach
- 📄 **Swagger** (opisy po polsku) - `http://localhost:8080/swagger-ui.html`

---

## 🗂️ Struktura backendu

```
com.example.arbomachine
├── domain.user
│   ├── entity      UserAccount, EmailConfirmationToken, PasswordResetToken
│   ├── enums       UserRole, AssignableUserRole (role do wyboru - bez ADMIN)
│   ├── dto         UserProfileDto, UserAccountDto, LoginRequestDto, ...
│   ├── service     UserRegistrationService, UserLoginService, EmailConfirmationService,
│   │               PasswordResetService, UserProfileUpdateService, UserAccountDeletionService,
│   │               UserAccountQueryService, AdminUserAccountManagementService
│   ├── facade      UserAccountFacade - operacje użytkownika na WŁASNYM koncie
│   ├── mapper      UserAccountMapper
│   └── exception   wyjątki domenowe (każdy opisuje swój status HTTP)
└── infrastructure
    ├── api         UserAccountController, AdminUserAccountController, RolePanelController,
    │               ApiExceptionHandler
    ├── security    SecurityConfig (reguły dostępu), JwtTokenService, JwtAuthenticationFilter, ...
    ├── repository  repozytoria Spring Data
    └── service     EmailSenderService (maile z szablonów Thymeleaf)
```

---

## 🛠️ Uruchomienie

### 1. Baza danych

Ten sam kontener Postgres co w pozostałych projektach (`pgDB` / `user` / `password` na `localhost:5432`).
Jeśli nie działa:

```bash
docker compose up -d
```

ArboMachine trzyma swoje tabele (`user_account`, `email_confirmation_token`, `password_reset_token`)
we własnym schemacie `arbomachine` - Hibernate zakłada go przy starcie, więc tabele nie kolidują
z innymi projektami w tej samej bazie.

### 2. Sekrety

Login/hasło SMTP i sekret JWT leżą w `src/main/resources/arbomachine-secrets.properties` (w `.gitignore`).
Wzór: `arbomachine-secrets.properties.example`. Jeśli SMTP nie działa, dopisz tam
`app.registration.email-confirmation-required=false` - konta będą aktywowane od razu, bez maila.

### 3. Backend

```bash
./mvnw spring-boot:run
```

Opcjonalne zmienne środowiskowe:

| Zmienna | Domyślnie | Znaczenie |
|---|---|---|
| `ARBOMACHINE_PORT` | `8080` | port backendu (na tym komputerze 8080 zajmuje `AgentService.exe` - użyj np. 8081) |
| `ARBOMACHINE_DB_URL` / `_DB_USER` / `_DB_PASSWORD` | `jdbc:postgresql://localhost:5432/pgDB` / `user` / `password` | baza danych |
| `ARBOMACHINE_FRONTEND_BASE_URL` | `http://localhost:5173` | adres frontendu wstawiany do linków w mailach |

Pozostałe ustawienia aplikacji (`application.properties`): `auth.jwt.token-validity-days`,
`app.password-reset.link-validity-minutes`, `app.mail.sender-display-name`.

### 4. Frontend

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
```

Vite przekierowuje `/api` na `http://localhost:8080`. Przy innym porcie backendu:
`API_PROXY_TARGET=http://localhost:8081 npm run dev`.

---

## 🔌 REST API

| Metoda | Ścieżka | Dostęp | Opis |
|---|---|---|---|
| POST | `/api/users/register` | publiczny | Rejestracja `{ profile, password, accountType: ARBORIST \| OFFICIAL }` |
| GET | `/api/users/confirm?token=` | publiczny | Potwierdzenie adresu e-mail |
| POST | `/api/users/login` | publiczny | Logowanie → `{ accessToken }` |
| POST | `/api/users/password-reset/request` | publiczny | Wysłanie linku resetu `{ email }` (zawsze 200) |
| GET | `/api/users/password-reset/validate?token=` | publiczny | Sprawdzenie tokenu resetu |
| POST | `/api/users/password-reset/confirm` | publiczny | Nowe hasło `{ resetToken, newPassword }` |
| GET / PUT / DELETE | `/api/users/me` | każdy zalogowany | Własne konto |
| GET | `/api/arborist/panel` | ARBORIST | Panel arborysty |
| GET | `/api/official/panel` | OFFICIAL | Panel urzędnika |
| GET | `/api/admin/panel`, `/api/admin/stats` | ADMIN | Panel admina i statystyki kont |
| GET | `/api/admin/users[?role=]`, `/api/admin/users/{userAccountId}` | ADMIN | Lista kont / pojedyncze konto |
| PUT | `/api/admin/users/{userAccountId}` | ADMIN | Edycja danych profilu |
| PATCH | `/api/admin/users/{userAccountId}/role` | ADMIN | `{ newRole: ARBORIST \| OFFICIAL }` |
| PATCH | `/api/admin/users/{userAccountId}/locked` | ADMIN | `{ lockedByAdmin: true \| false }` |
| POST | `/api/admin/users/{userAccountId}/confirm-email` | ADMIN | Potwierdzenie e-maila w imieniu użytkownika |
| DELETE | `/api/admin/users/{userAccountId}` | ADMIN | Usunięcie konta |

Brak tokenu → 401, zła rola → 403.

---

## 🧪 Testy

```bash
./mvnw test
```

- `SecurityRulesTest` sprawdza reguły dostępu na prawdziwym `SecurityConfig` (np. arborysta i urzędnik
  nie wejdą w `/api/admin/**` ani do cudzego panelu).
- Pozostałe testy kontrolerów działają w profilu `test`, w którym `PermitAllTestSecurityConfig`
  przepuszcza wszystkie żądania - dzięki temu sprawdzają logikę, a nie uprawnienia.
- `JwtTokenServiceTest` pilnuje m.in., żeby czas wystawienia tokenu nie wypadał w przyszłości
  (wcześniejszy błąd strefy czasowej).

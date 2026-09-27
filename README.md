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
- 📁 **Projekty arborysty** - lista własnych projektów (`/projects`), tworzenie projektu i workspace projektu
  (`/projects/:projectId`) z miejscem na moduły Drzewa / Mapa / Zdjęcia i dokumentacja
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
│   │               UserAccountQueryService, AdminUserAccountManagementService, RolePanelWelcomeService,
│   │               UserAccountDeletionGuard (punkt rozszerzenia dla innych modułów)
│   ├── facade      UserAccountFacade - operacje użytkownika na WŁASNYM koncie
│   ├── mapper      UserAccountMapper
│   └── exception   wyjątki domenowe (każdy opisuje swój status HTTP)
├── domain.project
│   ├── entity      Project (owner -> UserAccount)
│   ├── dto         CreateProjectRequest, UpdateProjectRequest, ProjectResponse
│   ├── service     ProjectService (punkt wejścia modułu), ProjectCreationService, ProjectQueryService,
│   │               ProjectUpdateService, ProjectOwnerAccountDeletionGuard
│   ├── mapper      ProjectMapper
│   └── exception   ProjectNotFoundException
├── domain.tree       (zależy od domain.project; własność drzewa = własność projektu)
│   ├── entity      Tree (project -> Project; bez własnego ownera)
│   ├── dto         CreateTreeRequest, TreeResponse
│   ├── service     TreeService (punkt wejścia modułu), TreeCreationService, TreeQueryService
│   ├── mapper      TreeMapper
│   └── exception   TreeNotFoundException
└── infrastructure
    ├── api         ApiExceptionHandler (wspólny) + podpakiety modułów:
    │   ├── user      UserAccountController
    │   ├── admin     AdminUserAccountController, AdminPanelController
    │   ├── arborist  ArboristPanelController
    │   ├── official  OfficialPanelController
    │   ├── project   ProjectController, ProjectApiExceptionHandler
    │   └── tree      TreeController, TreeApiExceptionHandler
    ├── security    SecurityConfig (reguły dostępu), JwtTokenService, JwtAuthenticationFilter, ...
    ├── repository  repozytoria Spring Data
    └── service     EmailSenderService (maile z szablonów Thymeleaf)
```

Każdy moduł ma własne pliki i nie sięga do wnętrza innych modułów. Moduł projektów korzysta z modułu
użytkowników tylko przez `UserAccountQueryService`, a blokadę usuwania kont dostarcza przez interfejs
`UserAccountDeletionGuard` - moduł użytkowników nie wie nic o projektach.

## 🗂️ Struktura frontendu

```
frontend/src
├── api          httpClient (wspólna warstwa HTTP + JWT), authenticationApi, currentUserAccountApi,
│                apiDataTypes, userFacingErrorMessages
├── auth         AuthenticationContext (sesja, logowanie), routeGuards, userRoles
├── components   wspólne klocki UI (bez wiedzy o konkretnych modułach)
└── pages
    ├── arborist   ArboristDashboardPage + arboristPanelApi
    ├── projects   ProjectsPage, NewProjectDialog, ProjectPage (workspace + sekcje jako trasy zagnieżdżone),
    │              ProjectWorkspaceSectionPlaceholder + projectsApi, projectDataTypes
    ├── trees      ProjectTreesSection, NewTreeDialog, TreePage + treesApi, treeDataTypes
    ├── official   OfficialDashboardPage + officialPanelApi
    ├── admin      AdminDashboardPage, AdminUserAccountsPage + adminPanelApi, adminUserAccountsApi
    └── ...        logowanie, rejestracja, reset hasła, MyProfilePage
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
| POST | `/api/projects` | każdy zalogowany | Nowy projekt `{ name, description? }` → 201; właścicielem zostaje zalogowany użytkownik |
| GET | `/api/projects` | każdy zalogowany | Tylko własne projekty (najnowsze pierwsze) |
| GET | `/api/projects/{projectId}` | każdy zalogowany | Własny projekt; cudzy albo nieistniejący → 404 |
| PATCH | `/api/projects/{projectId}` | każdy zalogowany | Zmiana `name` i/lub `description` własnego projektu; cudzy → 404 |
| POST | `/api/projects/{projectId}/trees` | każdy zalogowany | Nowe drzewo `{ inventoryNumber, speciesLatin?, notes? }` → 201; cudzy/nieistniejący projekt → 404 |
| GET | `/api/projects/{projectId}/trees` | każdy zalogowany | Drzewa własnego projektu w kolejności dodawania |
| GET | `/api/projects/{projectId}/trees/{treeId}` | każdy zalogowany | Drzewo tylko z tego projektu; drzewo z innego projektu → 404 |

Brak tokenu → 401, zła rola → 403. Cudzy projekt → 404 (tak samo jak nieistniejący - API nie zdradza,
że projekt istnieje). Konta, które jest właścicielem projektów, nie da się usunąć → 409 (tymczasowa
polityka do czasu ustalenia archiwizacji / przenoszenia własności).

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
- `ProjectApiIntegrationTest` i `ProjectOwnerAccountDeletionIntegrationTest` przechodzą pełną ścieżkę
  API projektów na prawdziwym `SecurityConfig` i prawdziwych tokenach JWT (użytkownicy A i B: własność,
  walidacja, brak dostępu dla niezalogowanych, blokada usuwania konta z projektami).

## 🗄️ Schemat bazy

Brak Flyway/Liquibase - schemat tworzy i aktualizuje Hibernate (`spring.jpa.hibernate.ddl-auto=update`)
w schemacie `arbomachine`. Nowa tabela pojawia się przy pierwszym starcie aplikacji po dodaniu encji.
Tabela `project`: `id bigint PK`, `owner_id bigint NOT NULL FK -> user_account(id)` (indeks),
`name varchar(255) NOT NULL`, `description text NULL`, `created_at` / `updated_at timestamptz NOT NULL`.
Tabela `tree`: `id bigint PK`, `project_id bigint NOT NULL FK -> project(id)` (indeks),
`inventory_number varchar(100) NOT NULL`, `species_latin varchar(255) NULL`, `notes text NULL`,
`created_at` / `updated_at timestamptz NOT NULL`. Bez `owner_id` i bez unikalności numeru - zasady numeracji
nie są jeszcze ustalone.

## 🧭 Trasy frontendu projektów

| Trasa | Dostęp | Zawartość |
|---|---|---|
| `/projects` | ARBORIST | Lista własnych projektów, przycisk „New project” (okno tworzenia) |
| `/projects/:projectId` | ARBORIST | Workspace projektu - sekcja „Trees”: lista drzew i „Add tree” |
| `/projects/:projectId/trees/:treeId` | ARBORIST | Podgląd drzewa (numer, gatunek, notatka) |
| `/projects/:projectId/map` | ARBORIST | Sekcja „Map” (placeholder, bez biblioteki mapowej) |
| `/projects/:projectId/documentation` | ARBORIST | Sekcja „Photos & documentation” (placeholder) |

Trasy korzystają z tego samego strażnika roli (`RequireSignedInUserWithRole`) i layoutu panelu co `/arborist`.
Kolejny moduł projektu (np. mapa) dostanie własną trasę zagnieżdżoną w `/projects/:projectId` -
bez zmian w `ProjectPage`.

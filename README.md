# 🌳 ArboMachine

**Czym jest (stan: Milestone 1 + etapy 17-18 - formularz terenowy i dyktowanie):** aplikacja webowa dla niezależnego arborysty do pracy terenowej
i biurowej nad projektem (np. inwentaryzacja drzew w parku). Arborysta zakłada konto, tworzy projekty, dodaje
drzewa z podstawowymi danymi, wskazuje ich położenie na mapie, dołącza zdjęcia i zapisuje zalecenia/prace, a na
podsumowaniu projektu widzi, ile materiału już zebrał. To pierwszy działający pionowy fragment produktu -
**nie** kompletny system arborystyczny (patrz „Czego jeszcze nie ma”).

**Stack:** backend Spring Boot 3.5 (**Java 25**, Spring Security + JWT, JPA/Hibernate, PostgreSQL, Thymeleaf dla maili),
frontend React 19 + TypeScript + Vite w katalogu [`frontend/`](frontend), mapa OpenLayers (podkład OpenStreetMap).
Zbudowany frontend serwuje Spring Boot (jeden proces, jeden port).

Konwencja językowa:
- **kod i identyfikatory techniczne po angielsku** - klasy, encje, metody, zmienne, DTO, komponenty React, typy
  TypeScript, pliki, endpointy REST, enumy i pola API (możliwie samoopisujące się, np. `UserAccount`,
  `ProjectTreeMap`, `setTreeLocation`);
- **cały interfejs użytkownika po polsku** - wszystkie ekrany, komunikaty błędów, powitania paneli i maile
  (rejestracja, reset hasła). Bez biblioteki i18n - teksty są wprost w komponentach;
- **komentarze i dokumentacja po polsku**. Surowe komunikaty błędów API są celowo po angielsku (kontrakt API) -
  frontend tłumaczy je w `userFacingErrorMessages.ts`.

## ✅ Milestone 1 - co działa

Rejestracja / logowanie → **Projekty** → nowy projekt → **Podsumowanie** projektu (liczby drzew, lokalizacji,
zdjęć, prac + szybkie akcje) → **Drzewa** → nowe drzewo → **karta drzewa** (dane Tree V1, **Lokalizacja** na mapie
albo ręcznie, **Zdjęcia**, **Zalecenia i prace**) → **Mapa** projektu (markery, przejście do karty) → powrót do
podsumowania. Cały flow działa przez UI (bez wpisywania adresów), po odświeżeniu (F5) każdej trasy, na desktopie,
tablecie i telefonie. Własność danych egzekwuje backend (`Tree → Project → owner`) - cudze dane dają 404.

## 🚫 Czego jeszcze nie ma

Świadomie **niezaimplementowane** (otwarte pytania w [`docs/DO_DOPRECYZOWANIA.md`](docs/DO_DOPRECYZOWANIA.md)):
ocena stanu drzewa / VTA / ryzyko, waloryzacja, gospodarka drzewostanem, słownik gatunków i typów prac, symbole prac
na mapie i legenda, adnotacje na zdjęciach, generator dokumentacji (PDF/DOCX), import/eksport (QGIS, GeoPackage, SHP,
XLSX), WMS/Geoportal, układy PL-2000/PL-1992, PostGIS, GNSS/RTK, praca offline, usuwanie drzew i projektów,
współpraca wielu użytkowników nad projektem (organizacje, urząd ↔ wykonawca). Panel urzędnika ma na razie tylko
pulpit z zapowiedziami modułów.

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
- 📁 **Projekty arborysty** - lista własnych projektów (`/projects`), tworzenie i edycja projektu, workspace
  projektu z zakładkami **Podsumowanie** / **Drzewa** / **Mapa** i breadcrumbem („Projekty / nazwa projektu”)
- 📊 **Podsumowanie projektu** (`/projects/:projectId`) - liczba drzew, drzew z lokalizacją i bez, zdjęć oraz
  zaleceń/prac (liczone w bazie jednym zestawem zapytań COUNT) + szybkie akcje „Przejdź do drzew” / „Otwórz mapę”
- 🌳 **Drzewa projektu** (`/projects/:projectId/trees`) - lista drzew; „Dodaj drzewo” pyta tylko o numer i od razu
  otwiera kartę drzewa. Na karcie **formularz terenowy** (tablet-first, zawsze edytowalny): Identyfikacja → Gatunek →
  Pomiary (klawiatura numeryczna, przecinek dziesiętny, jednostki w etykietach) → Stan drzewa → Zagrożenia → Notatki;
  „Zapisz zmiany” / „Anuluj”, sygnał niezapisanych zmian, „Dodaj następne drzewo”.
  Model **Tree V1**: numer inwentaryzacyjny, gatunek łaciński i polski, obwód pnia (cm), wysokość (m),
  średnica korony (m), notatka, opis stanu i opis zagrożeń (wolny tekst V1). To roboczy model do rozwijania
  małymi krokami, nie docelowy model arborystyczny -
  patrz [`docs/discovery/etap-8-karta-drzewa-discovery.md`](docs/discovery/etap-8-karta-drzewa-discovery.md)
- 📍 **Lokalizacja drzewa (TreeLocation V1)** - punkt WGS84 (`latitude`/`longitude` w stopniach dziesiętnych),
  ustawiany w karcie drzewa wskazaniem na mapie albo ręcznym formularzem współrzędnych; można go też usunąć
- 🗺️ **Mapa projektu** (`/projects/:projectId/map`, OpenLayers + podkład OpenStreetMap) - markery drzew
  z zapisaną lokalizacją; kliknięcie markera pokazuje „Drzewo {numer}”, gatunek i link do karty drzewa
- 📷 **Zdjęcia drzewa (TreePhoto V1)** - sekcja „Zdjęcia” w karcie drzewa: dodawanie z urządzenia (na tablecie
  przeglądarka może od razu otworzyć aparat), siatka miniatur, większy podgląd z pobraniem, usuwanie z potwierdzeniem.
  JPEG/PNG/WebP, typ sprawdzany z zawartości pliku, limit 15 MB (konfigurowalny); pliki na dysku, metadane w bazie
- 🪚 **Zalecenia i prace (TreeWork V1)** - sekcja „Zalecenia i prace” w karcie drzewa: wiele prac na drzewo,
  każda z nazwą (wolny tekst, np. „Cięcia redukcyjne”) i opcjonalnym opisem; dodawanie, edycja, usuwanie.
  Celowo bez słownika typów prac, symboli mapowych i priorytetów (`docs/DO_DOPRECYZOWANIA.md`)
- 🛠️ **Administracja** - statystyki, lista kont z filtrami, akcje na kontach
- 📄 **Swagger** (opisy po polsku) - `http://localhost:<port>/swagger-ui.html`
- 🎤 **Dyktowanie (Voice V1)** - przycisk „Dyktuj” przy polach opisowych (stan, zagrożenia, notatki drzewa, opis
  pracy): Web Speech API przeglądarki, `pl-PL`, tekst dopisywany do pola do poprawienia przed zapisem, bez
  autozapisu, bez nagrań audio i bez AI. Przeglądarka bez wsparcia (np. Firefox) - przycisk ukryty
- ♿ **Podstawy dostępności** - etykiety pól, `alt` zdjęć, okna dialogowe z Escape, pułapką fokusu i powrotem fokusu

---

## 🗂️ Struktura backendu

Architektura: **kontroler → fasada → handlery**.

- Każdy moduł ma folder `facade/` z fasadami (`XxxFacade`). Kontrolery wstrzykują **wyłącznie fasady**.
- Folder `service/` zawiera podfoldery nazwane **dokładnie jak fasada, małymi literami**
  (`TreeFacade` → `service/treefacade/`). W środku leżą handlery, z których składa się dana fasada.
- Handler = jedna operacja = **jedna publiczna metoda `handle(...)`**. Brak wielometodowych serwisów.
- Reguły pilnuje test `FacadeHandlerArchitectureTest` (build nie przejdzie, jeśli ktoś je złamie).

```
com.example.arbomachine
├── domain.user
│   ├── entity      UserAccount, EmailConfirmationToken, PasswordResetToken
│   ├── enums       UserRole, AssignableUserRole (role do wyboru - bez ADMIN)
│   ├── dto         UserProfileDto, UserAccountDto, LoginRequestDto, ...
│   ├── facade      UserAccountFacade (własne konto), UserAuthenticationFacade (rejestracja, e-mail, logowanie),
│   │               PasswordResetFacade, AdminUserAccountFacade, RolePanelWelcomeFacade
│   ├── service
│   │   ├── useraccountfacade        GetCurrentUserAccountHandler, UpdateCurrentUserProfileHandler,
│   │   │                            DeleteCurrentUserAccountHandler, FindUserAccountByEmailHandler, ...,
│   │   │                            UserAccountDeletionGuard (punkt rozszerzenia dla innych modułów)
│   │   ├── userauthenticationfacade RegisterNewUserAccountHandler, ConfirmEmailUsingTokenHandler,
│   │   │                            LoginAndIssueAccessTokenHandler
│   │   ├── passwordresetfacade      SendPasswordResetLinkIfAccountExistsHandler, ResetPasswordUsingTokenHandler, ...
│   │   ├── adminuseraccountfacade   ListUserAccountsHandler, ChangeUserRoleHandler, SetAccountLockedByAdminHandler, ...
│   │   └── rolepanelwelcomefacade   BuildRolePanelWelcomeHandler
│   ├── mapper      UserAccountMapper
│   └── exception   wyjątki domenowe (każdy opisuje swój status HTTP)
├── domain.project
│   ├── entity      Project (owner -> UserAccount)
│   ├── dto         CreateProjectRequest, UpdateProjectRequest, ProjectResponse
│   ├── facade      ProjectFacade
│   ├── service.projectfacade  CreateProjectHandler, GetProjectsOfCurrentUserHandler, GetProjectOfCurrentUserHandler,
│   │                          UpdateProjectHandler, FindProjectOwnedByCurrentUserHandler (jedyne miejsce
│   │                          sprawdzania własności), ProjectOwnerAccountDeletionGuard
│   ├── mapper      ProjectMapper
│   └── exception   ProjectNotFoundException
├── domain.tree       (zależy od domain.project; własność drzewa = własność projektu)
│   ├── entity      Tree (project -> Project; bez własnego ownera)
│   ├── entity      + TreeEditableAttributes (komplet pól edytowalnych - porównanie "czy coś się zmieniło")
│   ├── dto         CreateTreeRequest, UpdateTreeRequest (oba: TreeEditableFieldsRequest), TreeResponse
│   ├── facade      TreeFacade
│   ├── service.treefacade  CreateTreeHandler, GetTreesOfProjectHandler, GetTreeHandler, UpdateTreeHandler,
│   │                       FindTreeInProjectOwnedByCurrentUserHandler, TreeEditableAttributesNormalizer
│   ├── mapper      TreeMapper
│   └── exception   TreeNotFoundException
├── domain.treephoto  (zależy od domain.tree; własność = Tree -> Project -> owner)
│   ├── entity      TreePhoto (tree -> Tree; tylko metadane, plik w PhotoStorage)
│   ├── config      TreePhotoProperties (app.tree-photos.*: limit rozmiaru, katalog storage)
│   ├── storage     PhotoStorage (store/load/delete po kluczu) + PhotoStorageException
│   ├── dto         TreePhotoUpload (wejście bez MultipartFile), TreePhotoResponse, TreePhotoContent
│   ├── facade      TreePhotoFacade
│   ├── service.treephotofacade  UploadTreePhotoHandler, GetTreePhotosHandler, GetTreePhotoContentHandler,
│   │                            DeleteTreePhotoHandler, FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler,
│   │                            SupportedTreePhotoType (typ z sygnatury pliku)
│   ├── mapper      TreePhotoMapper
│   └── exception   TreePhotoNotFoundException, TreePhotoFileMissingException, TreePhotoFileTooLargeException,
│                   UnsupportedTreePhotoTypeException
├── domain.projectsummary  (read-model; zależy od project, tree, treephoto, treework - tylko przez COUNT w repozytoriach)
│   ├── dto         ProjectSummaryResponse
│   ├── facade      ProjectSummaryFacade
│   └── service.projectsummaryfacade  GetProjectSummaryHandler (własność przez FindProjectOwnedByCurrentUserHandler)
├── domain.treework   (zależy od domain.tree; własność = Tree -> Project -> owner)
│   ├── entity      TreeWork (tree -> Tree; name + description)
│   ├── dto         CreateTreeWorkRequest, UpdateTreeWorkRequest (oba: TreeWorkFieldsRequest), TreeWorkResponse
│   ├── facade      TreeWorkFacade
│   ├── service.treeworkfacade  CreateTreeWorkHandler, GetTreeWorksHandler, GetTreeWorkHandler, UpdateTreeWorkHandler,
│   │                           DeleteTreeWorkHandler, FindTreeWorkOfTreeInProjectOwnedByCurrentUserHandler,
│   │                           TreeWorkFieldsNormalizer
│   ├── mapper      TreeWorkMapper
│   └── exception   TreeWorkNotFoundException
└── infrastructure
    ├── api         ApiExceptionHandler (wspólny) + podpakiety modułów:
    │   ├── user      UserAccountController
    │   ├── admin     AdminUserAccountController, AdminPanelController
    │   ├── arborist  ArboristPanelController
    │   ├── official  OfficialPanelController
    │   ├── project   ProjectController, ProjectApiExceptionHandler
    │   ├── projectsummary ProjectSummaryController, ProjectSummaryApiExceptionHandler
    │   ├── tree      TreeController, TreeApiExceptionHandler
    │   ├── treephoto TreePhotoController, TreePhotoApiExceptionHandler
    │   └── treework  TreeWorkController, TreeWorkApiExceptionHandler
    ├── security    SecurityConfig (reguły dostępu), JwtTokenService, JwtAuthenticationFilter, ...
    ├── repository  repozytoria Spring Data
    ├── storage     FileSystemPhotoStorage (PhotoStorage na dysku), TreePhotoUploadConfig (limit multipart z limitu zdjęcia)
    └── service     EmailSenderService (maile z szablonów Thymeleaf)
```

Każdy moduł ma własne pliki. Moduły projektów i drzew korzystają z modułu użytkowników tylko przez
`FindUserAccountByEmailHandler`, a moduł drzew sprawdza własność projektu wyłącznie przez
`FindProjectOwnedByCurrentUserHandler`. Moduły zdjęć i prac sprawdzają całą hierarchię wyłącznie przez
`FindTreeInProjectOwnedByCurrentUserHandler` (drzewo z URL w projekcie z URL należącym do zalogowanego), a potem
szukają zdjęcia/pracy zawężonego do tego drzewa (`findByIdAndTreeId`) - nigdy samym `findById`. Blokadę usuwania kont moduł projektów dostarcza przez interfejs
`UserAccountDeletionGuard` - moduł użytkowników nie wie nic o projektach.

## 🗂️ Struktura frontendu

```
frontend/src
├── api          httpClient (wspólna warstwa HTTP + JWT), authenticationApi, currentUserAccountApi,
│                apiDataTypes, userFacingErrorMessages
├── auth         AuthenticationContext (sesja, logowanie), routeGuards, userRoles
├── components   wspólne klocki UI (bez wiedzy o konkretnych modułach)
├── map          openLayersBaseMap - wspólna techniczna konfiguracja OpenLayers (podkład OSM, widok, styl markera)
├── speech       dyktowanie: browserSpeechRecognition (jedyne miejsce znające Web Speech API), DictationController
│                (logika bez Reacta, jedna sesja naraz), useSpeechDictation, VoiceInputButton,
│                VoiceEnabledTextAreaField, appendTranscription (dopisywanie bez kasowania)
└── pages
    ├── arborist   ArboristDashboardPage + arboristPanelApi
    ├── projects   ProjectsPage, NewProjectDialog, EditProjectDialog, ProjectPage (workspace: breadcrumb, zakładki,
    │   │          sekcje jako trasy zagnieżdżone), RedirectToProjectSummary + projectsApi, projectDataTypes
    │   └── summary   ProjectSummaryPage, projectSummaryStatistics (kafelki), projectSummaryApi
    ├── trees      ProjectTreesSection, NewTreeDialog (sam numer -> karta), TreePage (karta drzewa: sekcje),
    │   │          TreeFieldForm (formularz terenowy w sekcjach) + treeFormValues (wartości, przecinek dziesiętny,
    │   │          walidacja) + treesApi, treeDataTypes
    │   ├── location  TreeLocationSection (tryby: podgląd / ręczny formularz / wybór na mapie), TreeLocationForm,
    │   │             TreeLocationMap, RemoveTreeLocationDialog, treeLocationMapCoordinates (WGS84 <-> mapa),
    │   │             treeLocationValidRange, treeLocationFormValues
    │   ├── map       ProjectTreeMapSection (trasa /map: dane i stany UI), ProjectTreeMap (OpenLayers),
    │   │             treeMapPoints (drzewa -> punkty mapy)
    │   ├── photos    TreePhotosSection (galeria), AddTreePhotoDialog, TreePhotoTile, DeleteTreePhotoDialog,
    │   │             useTreePhotoObjectUrl (treść z tokenem -> blob:), treePhotoFileRules + treePhotosApi
    │   └── works     TreeWorksSection (lista + edycja w miejscu), TreeWorkForm, DeleteTreeWorkDialog,
    │                 treeWorkFormValues (walidacja) + treeWorksApi
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

Wymaga **JDK 25** (w IntelliJ: Project SDK = 25).

```bash
./mvnw spring-boot:run
```

Po `npm run build` w `frontend/` backend serwuje też aplikację: `http://localhost:<port>/` (logowanie, projekty,
wszystkie trasy React - także po F5). Bez zbudowanego frontendu główny adres przekierowuje do Swagger UI.
Do pracy nad frontendem z hot reload służy `npm run dev` (punkt 4).

Opcjonalne zmienne środowiskowe:

| Zmienna | Domyślnie | Znaczenie |
|---|---|---|
| `ARBOMACHINE_PORT` | `8080` | port backendu (na tym komputerze 8080 zajmuje `AgentService.exe` - użyj np. 8081) |
| `ARBOMACHINE_DB_URL` / `_DB_USER` / `_DB_PASSWORD` | `jdbc:postgresql://localhost:5432/pgDB` / `user` / `password` | baza danych |
| `ARBOMACHINE_FRONTEND_BASE_URL` | `http://localhost:5173` | adres frontendu wstawiany do linków w mailach |
| `ARBOMACHINE_TREE_PHOTOS_DIRECTORY` | `storage/tree-photos` | katalog plików zdjęć drzew (względny = od katalogu roboczego; w `.gitignore`) |
| `ARBOMACHINE_TREE_PHOTO_MAX_FILE_SIZE` | `15MB` | limit jednego zdjęcia (`app.tree-photos.max-file-size`); z niego liczony jest też limit multipart serwera |

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

```bash
npm test             # testy logiki (node --test, bez dodatkowych zależności)
npm run build        # tsc + build do src/main/resources/static (serwuje go Spring Boot)
```

### 🗺️ Mapa (OpenLayers)

Jedyną biblioteką mapową jest **OpenLayers** (`ol`) - wybrana pod przyszły workflow GIS (WMS, Geoportal,
układy PL-2000/PL-1992, QGIS), które jednak **nie są jeszcze zaimplementowane**. Podkład V1 to OpenStreetMap.

- Dane w bazie i API zostają w **WGS84** (`latitude`/`longitude`). OpenLayers renderuje w EPSG:3857 -
  transformacja odbywa się wyłącznie na potrzeby mapy w `treeLocationMapCoordinates.ts` (jedyne miejsce zamiany;
  OpenLayers używa kolejności `[longitude, latitude]`, API `{ latitude, longitude }`). Współrzędnych projekcji
  mapy nigdy nie wysyłamy do backendu.
- **Mapa projektu** korzysta z `GET /api/projects/{projectId}/trees` (odpowiedź zawiera już `location`) -
  bez osobnego endpointu mapy i bez zapytań o każde drzewo. Widok dopasowuje się do markerów (jedno drzewo -
  zoom 18, a nie ekstremalny); bez lokalizacji - widok całej Polski i komunikat „Brak drzew z zapisaną lokalizacją.”.
  Geolokalizacja przeglądarki nie jest używana.
- **Ustawianie lokalizacji na mapie** (karta drzewa, sekcja „Lokalizacja”): „Ustaw na mapie” / „Zmień na mapie”
  → kliknięcie stawia tymczasowy (pomarańczowy) punkt → „Zapisz lokalizację” albo „Anuluj”. Dopiero zapis wysyła
  istniejące `PUT .../trees/{treeId}/location`; po sukcesie UI pokazuje punkt zwrócony przez backend
  (backend zaokrągla do 7 miejsc). Ręczny formularz („Wpisz współrzędne”) zostaje i zmienia ten sam `TreeLocation`;
  karta drzewa trzyma jedno źródło prawdy o drzewie, więc mapa i formularz zawsze pokazują te same dane.

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
| POST | `/api/projects/{projectId}/trees` | każdy zalogowany | Nowe drzewo `{ inventoryNumber, speciesLatin?, speciesPolish?, trunkCircumferenceCm?, heightM?, crownDiameterM?, notes?, conditionNotes?, hazardNotes? }` → 201; cudzy/nieistniejący projekt → 404 |
| GET | `/api/projects/{projectId}/trees` | każdy zalogowany | Drzewa własnego projektu w kolejności dodawania |
| GET | `/api/projects/{projectId}/trees/{treeId}` | każdy zalogowany | Drzewo tylko z tego projektu; drzewo z innego projektu → 404 |
| PATCH | `/api/projects/{projectId}/trees/{treeId}` | każdy zalogowany | Edycja - te same pola co przy tworzeniu → 200 z aktualnym drzewem. Wysyłany jest zawsze komplet pól: pominięte/puste pole opcjonalne = wyczyszczenie. Nie da się przenieść drzewa do innego projektu; `updatedAt` zmienia się tylko przy realnej zmianie |
| GET | `/api/projects/{projectId}/summary` | każdy zalogowany | Podsumowanie własnego projektu `{ treeCount, treeWithLocationCount, treeWithoutLocationCount, photoCount, workCount }` (COUNT w bazie); cudzy → 404 |
| PUT | `/api/projects/{projectId}/trees/{treeId}/location` | każdy zalogowany | Ustawienie/zamiana punktu `{ latitude, longitude }` (WGS84, oba wymagane) → 200 z drzewem; używają go i mapa, i ręczny formularz |
| DELETE | `/api/projects/{projectId}/trees/{treeId}/location` | każdy zalogowany | Usunięcie lokalizacji (`location: null`) → 200 z drzewem |
| POST | `/api/projects/{projectId}/trees/{treeId}/photos` | każdy zalogowany | Dodanie zdjęcia: `multipart/form-data`, pole `file` (JPEG/PNG/WebP) → 201 z metadanymi; brak pliku → 400, zły typ → 415, za duży → 413 |
| GET | `/api/projects/{projectId}/trees/{treeId}/photos` | każdy zalogowany | Metadane zdjęć drzewa `{ id, treeId, originalFileName, contentType, fileSizeBytes, createdAt }` (bez ścieżek plików) |
| GET | `/api/projects/{projectId}/trees/{treeId}/photos/{photoId}/content` | każdy zalogowany | Treść zdjęcia (`Content-Disposition: inline`) |
| DELETE | `/api/projects/{projectId}/trees/{treeId}/photos/{photoId}` | każdy zalogowany | Usunięcie zdjęcia (rekord i plik) → 204 |
| POST | `/api/projects/{projectId}/trees/{treeId}/works` | każdy zalogowany | Nowa praca `{ name, description? }` → 201 |
| GET | `/api/projects/{projectId}/trees/{treeId}/works` | każdy zalogowany | Prace drzewa w kolejności dodawania |
| GET | `/api/projects/{projectId}/trees/{treeId}/works/{workId}` | każdy zalogowany | Pojedyncza praca |
| PATCH | `/api/projects/{projectId}/trees/{treeId}/works/{workId}` | każdy zalogowany | Edycja - komplet pól `{ name, description? }` (pominięty opis = wyczyszczenie) → 200 |
| DELETE | `/api/projects/{projectId}/trees/{treeId}/works/{workId}` | każdy zalogowany | Usunięcie pracy → 204 |

Brak tokenu → 401, zła rola → 403. Cudzy projekt → 404 (tak samo jak nieistniejący - API nie zdradza,
że projekt istnieje). To samo dotyczy drzewa spoza projektu z URL oraz zdjęcia/pracy spoza drzewa z URL. Konta, które jest właścicielem projektów, nie da się usunąć → 409 (tymczasowa
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
- `TreeApiIntegrationTest` - to samo dla drzew, w tym edycja (błędny `projectId`, cudzy projekt,
  czyszczenie pól opcjonalnych, `updatedAt` tylko przy realnej zmianie).
- `TreePhotoApiIntegrationTest` - zdjęcia: upload/lista/treść/usuwanie, właściciel vs użytkownik B, zdjęcie innego
  drzewa w URL, zły typ (także plik `.jpg` z inną zawartością), za duży plik, brak pliku, niebezpieczna nazwa pliku.
  Pliki trafiają do katalogu tymczasowego (`@TempDir`), nigdy do developerskiego storage.
- `FileSystemPhotoStorageTest` - zapis/odczyt/usuwanie, brak nadpisywania, odrzucanie kluczy typu `../`.
- `TreeWorkApiIntegrationTest` - prace: CRUD, walidacja nazwy/opisu, użytkownik B, praca innego drzewa w URL,
  nieistniejący projekt/drzewo/praca.
- `ProjectSummaryApiIntegrationTest` - podsumowanie: pusty projekt, przykład milestone'u (3 drzewa → 3/2/1/3/3),
  suma lokalizacji = liczba drzew, brak wpływu innych projektów, stała liczba zapytań SQL (brak N+1), własność.
- `MilestoneOwnershipRegressionIntegrationTest` - użytkownik B próbuje każdego endpointu na danych A
  (projekt, podsumowanie, drzewo, lokalizacja, zdjęcie i jego treść, praca) → zawsze 404, dane A bez zmian;
  bez tokenu → 401.
- `FacadeHandlerArchitectureTest` - reguły architektury kontroler → fasada → handler.

## 🗄️ Schemat bazy

Brak Flyway/Liquibase - schemat tworzy i aktualizuje Hibernate (`spring.jpa.hibernate.ddl-auto=update`)
w schemacie `arbomachine`. Nowa tabela pojawia się przy pierwszym starcie aplikacji po dodaniu encji.
Tabela `project`: `id bigint PK`, `owner_id bigint NOT NULL FK -> user_account(id)` (indeks),
`name varchar(255) NOT NULL`, `description text NULL`, `created_at` / `updated_at timestamptz NOT NULL`.
Tabela `tree`: `id bigint PK`, `project_id bigint NOT NULL FK -> project(id)` (indeks),
`inventory_number varchar(100) NOT NULL`, `species_latin varchar(255) NULL`, `species_polish varchar(255) NULL`,
`trunk_circumference_cm integer NULL` (1-5000), `height_m numeric(5,2) NULL` (>0 do 150),
`crown_diameter_m numeric(5,2) NULL` (>0 do 200), `notes text NULL`, `condition_notes text NULL` (stan drzewa, V1),
`hazard_notes text NULL` (zagrożenia, V1), `created_at` / `updated_at timestamptz NOT NULL`.
Jednostka pomiaru jest w nazwie kolumny. Bez `owner_id` i bez unikalności numeru - zasady numeracji
nie są jeszcze ustalone. Nowe pola drzewa są nullable (dane terenowe mogą być uzupełniane później), więc
`ddl-auto=update` dodaje je do istniejącej tabeli bez utraty danych.
Tabela `tree_photo`: `id bigint PK`, `tree_id bigint NOT NULL FK -> tree(id)` (indeks),
`original_file_name varchar(255) NOT NULL`, `storage_key varchar(64) NOT NULL UNIQUE`, `content_type varchar(100) NOT NULL`,
`file_size_bytes bigint NOT NULL`, `created_at timestamptz NOT NULL` - sam plik leży poza bazą.
Tabela `tree_work`: `id bigint PK`, `tree_id bigint NOT NULL FK -> tree(id)` (indeks), `name varchar(255) NOT NULL`,
`description text NULL`, `created_at` / `updated_at timestamptz NOT NULL`. Obie tabele bez `owner_id`.

## 🧭 Trasy frontendu projektów

| Trasa | Dostęp | Zawartość |
|---|---|---|
| `/projects` | ARBORIST | Lista własnych projektów, przycisk „Nowy projekt” (okno tworzenia) |
| `/projects/:projectId` | ARBORIST | **Podsumowanie projektu** (strona główna projektu): liczby i szybkie akcje |
| `/projects/:projectId/trees` | ARBORIST | Sekcja „Drzewa”: lista drzew i „Dodaj drzewo” |
| `/projects/:projectId/trees/:treeId` | ARBORIST | Karta drzewa: podgląd i edycja (Zapisz / Anuluj) oraz sekcje „Lokalizacja” (mapa + ręczny formularz), „Zdjęcia” i „Zalecenia i prace” |
| `/projects/:projectId/map` | ARBORIST | Sekcja „Mapa”: mapa OpenLayers z markerami drzew projektu |
| `/projects/:projectId/documentation` | ARBORIST | Dawna zakładka-placeholder - przekierowuje na podsumowanie (stare linki działają) |

Trasy korzystają z tego samego strażnika roli (`RequireSignedInUserWithRole`) i layoutu panelu co `/arborist`.
Kolejny moduł projektu dostanie własną trasę zagnieżdżoną w `/projects/:projectId` -
bez zmian w `ProjectPage`.

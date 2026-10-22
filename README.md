# 🌳 ArboMachine

**Aplikacja webowa dla niezależnego arborysty** - praca w terenie (tablet, telefon) i w biurze nad projektem, np.
inwentaryzacją drzew w parku: drzewa z pomiarami, lokalizacją GPS i zdjęciami, waloryzacja, zalecenia i plan prac,
kontrole stanu, grupy drzew, mapa, dokument PDF oraz wymiana danych z QGIS i aplikacjami terenowymi.

**Stan: MVP po etapach 1-30** - gotowe do pokazania arborystom i zebrania uwag. To **nie** jest kompletny system
arborystyczny: wszystko, czego nie wiemy od arborystów (metody waloryzacji, VTA, skale ocen, słowniki prac), jest
zapisane jako robocze „V1” albo pytanie w [`docs/DO_DOPRECYZOWANIA.md`](docs/DO_DOPRECYZOWANIA.md).

**Stack:** Spring Boot 3.5 (**Java 25**, Spring Security + JWT, JPA/Hibernate, PostgreSQL, Thymeleaf), React 19 +
TypeScript + Vite ([`frontend/`](frontend)), OpenLayers (podkład OpenStreetMap). Zbudowany frontend serwuje Spring Boot -
jeden proces, jeden port. PDF: openhtmltopdf, XLSX: fastexcel, GeoPackage: NGA, reszta formatów GIS i Web Push - własny
kod na bibliotece standardowej Javy.

Konwencja językowa: **kod i identyfikatory po angielsku**, **interfejs, komentarze i dokumentacja po polsku**. Surowe
komunikaty błędów API są po angielsku (kontrakt), frontend tłumaczy je w `userFacingErrorMessages.ts`.

---

## ✅ Co działa (przepływ MVP)

**Projekt → teren → waloryzacja i plan prac → mapa → dokument → eksport / QGIS**

| Obszar | Co jest | Szczegóły |
|---|---|---|
| 👥 Konta | rejestracja z potwierdzeniem e-mail, logowanie JWT, reset hasła, panele: arborysta / urzędnik / administrator | arborysta startuje od „Projekty”; urzędnik - na razie tylko pulpit |
| 📁 Projekty | lista, tworzenie, edycja; **dane zlecenia**: zleceniodawca, adres obiektu, data zlecenia; podsumowanie z liczbami | etap 29 |
| 🌳 Drzewa | formularz terenowy (tablet-first): numer, gatunek, obwód, wysokość, korona, stan, zagrożenia, notatki; dyktowanie | [etap 8](docs/discovery/etap-8-karta-drzewa-discovery.md) |
| 📍 Lokalizacja | punkt WGS 84 z mapy, ręcznie albo z **GPS urządzenia** (dokładność, wysokość, źródło); **pomiar uśredniony**, progi RTK, odbiornik RTK przez pozorowaną lokalizację Androida; współrzędne **PL-2000 / PL-1992** na karcie i w PDF, wpisywanie w układzie krajowym | [etap 25](docs/discovery/etap-25-gnss.md), 31 |
| 🏷️ Oznaczenia drzew | **wiele symboli na jednym drzewie** (wartościowe, pomnik przyrody, do usunięcia, złom, dziupla, gniazdo…), także offline; widok mapy, filtr listy, eksport, PDF | etap 31 |
| 📷 Zdjęcia | zdjęcia drzewa z aparatu tabletu, **adnotacje** (rysunek na zdjęciu bez zmiany oryginału) | etapy 13, 20 |
| 🏅 Waloryzacja | zapis **oceny arborysty**: kategoria V1 + opisy wartości (wiekowa, przyrodnicza, ...). **Bez wzorów i kwot** | [etap 27](docs/discovery/etap-27-waloryzacja.md) |
| 🪓 Gospodarka drzewostanem | zalecenia i prace: status, priorytet, termin, wykonanie; widok projektu z filtrami (też „najbliższe 7 dni”) | [etap 28](docs/discovery/etap-28-gospodarka-drzewostanem.md) |
| 🩺 Monitoring stanu | historia kontroli drzewa: data, ocena V1 (Dobry / Umiarkowany / Zły - nie VTA), uwagi; „Nie sprawdzone” na liście | [etap 29](docs/discovery/etap-29-monitoring-i-gis.md) |
| 🌲 Grupy drzew | obszary (szpalery, zadrzewienia) rysowane na mapie albo importowane; przypisanie drzew, także „w obszarze” | etap 29 |
| 🗺️ Mapa | tryby: symbole prac / **oznaczenia** / waloryzacja / gospodarka; **gwiazdka = iglaste**, warstwa grup drzew, legendy; **„Dodaj drzewo na mapie”** i „Pokaż na mapie” z karty drzewa | etapy 19, 27-29, 31 |
| 🛰️ Podkłady | OpenStreetMap, **ortofotomapa i mapa topograficzna Geoportalu**, nakładka **działek ewidencyjnych** (KIEG); **warstwy referencyjne z QGIS** (GeoJSON / SHP / GPKG / DXF) pod drzewami | etap 31 |
| 📄 Dokument | „Dokument projektu” PDF: dane zlecenia, podsumowanie, zestawienie drzew, gospodarka, grupy, karty drzew ze zdjęciami | etap 22 + rozszerzenia |
| 🔁 Import | CSV, XLSX (także **arkusze z aplikacji terenowych**), GeoJSON, **Shapefile (ZIP), GeoPackage, DXF**; **PL-2000 / PL-1992** przeliczane do WGS 84; obszary grup drzew | [etapy 23-24](docs/discovery/etap-23-import-export.md), 29 |
| 📤 Eksport / QGIS | CSV, XLSX (arkusze: Drzewa, Prace, Monitoring, Grupy drzew, Projekt), GeoJSON, **GeoPackage** (trees + stands), **Shapefile ZIP** | [instrukcja QGIS](docs/integrations/qgis.md) |
| 🔔 Powiadomienia | lista powiadomień + **Web Push**: ocena „Zły” przy kontroli, przypomnienie rano o pracach na jutro, test | [etap 30](docs/discovery/etap-30-integracje-i-finalizacja.md) |
| 📅 Kalendarz | eksport prac z terminami do **.ics** (Google / Outlook / Apple) | etap 30 |
| 📴 Offline | odczyty z pamięci urządzenia, kolejka zmian (drzewa, lokalizacja, prace, waloryzacja, kontrole) i synchronizacja | [etap 26](docs/discovery/etap-26-offline.md) |

Własność danych egzekwuje backend (`… → Tree → Project → owner`): cudze dane zawsze dają 404, także przy imporcie,
eksporcie, grupach drzew i powiadomieniach.

## 🚫 Czego (jeszcze) nie ma

Ocena stanu wg metody arborystycznej (VTA, klasy ryzyka), metoda waloryzacji i wycena pieniężna, słowniki gatunków i
rodzajów prac, harmonogram / kosztorysy / wykonawcy, DOCX, DWG / QGZ / import formatu projektu aplikacji terenowej,
WMS / Geoportal / ortofotomapy (także offline), PostGIS, RTK i odbiorniki zewnętrzne, zdjęcia i grupy drzew offline,
powiadomienia e-mail / SMS, usuwanie drzew i projektów, praca zespołowa nad projektem (organizacje, urząd ↔ wykonawca).
Pełny rejestr: [`docs/DO_DOPRECYZOWANIA.md`](docs/DO_DOPRECYZOWANIA.md) → „Rejestr braków”.

---

## 🛠️ Szybki start (deweloperski)

1. **Baza** - PostgreSQL na `localhost:5432` (`pgDB` / `user` / `password`, schemat `arbomachine` zakłada Hibernate):
   `docker compose up -d` (albo `docker start offerbrowserprototype-postgres-1`, jeśli kontener istnieje - nie oba naraz).
2. **Sekrety** - `src/main/resources/arbomachine-secrets.properties` (w `.gitignore`, wzór `*.example`): SMTP i sekret JWT.
   Bez SMTP: `app.registration.email-confirmation-required=false`.
3. **Backend** (JDK 25): `./mvnw spring-boot:run` - port `ARBOMACHINE_PORT` (domyślnie 8080).
4. **Frontend**: `cd frontend && npm install && npm run build` - backend serwuje aplikację pod `http://localhost:<port>/`.
   Hot reload: `npm run dev` (http://localhost:5173, `/api` przekierowane na `API_PROXY_TARGET`, domyślnie :8080).
5. **Dane do pokazu** (konto musi istnieć):
   `node scripts/demo/seed-demo-project.mjs --base http://localhost:<port> --email ... --password ... [--sheet plik.xlsx] [--drawing plik.dxf]`

Build wydania jednym poleceniem: `powershell -File scripts/build-release.ps1` albo `bash scripts/build-release.sh`
(testy + frontend + JAR). Wdrożenie, kopie zapasowe i wycofanie wersji: [`docs/WYDANIE.md`](docs/WYDANIE.md).

### Konfiguracja (zmienne środowiskowe / properties)

| Ustawienie | Domyślnie | Znaczenie |
|---|---|---|
| `ARBOMACHINE_PORT` | `8080` | port aplikacji |
| `ARBOMACHINE_DB_URL` / `_DB_USER` / `_DB_PASSWORD` | lokalny PostgreSQL | baza danych |
| `ARBOMACHINE_FRONTEND_BASE_URL` | adres aplikacji | adres w linkach z maili |
| `ARBOMACHINE_TREE_PHOTOS_DIRECTORY` / `_TREE_PHOTO_MAX_FILE_SIZE` | `storage/tree-photos` / `15MB` | pliki zdjęć |
| `app.push.vapid-public-key` / `app.push.vapid-private-key` | brak = klucze wygenerowane i zapisane w bazie | stałe klucze Web Push (zmiana = utrata subskrypcji) |
| `app.push.vapid-subject` | `mailto:powiadomienia@arbomachine.example` | kontakt dla usług push - **ustaw prawdziwy adres** |
| `app.push.allowed-endpoint-hosts` | FCM, Mozilla, Microsoft, Apple | dozwolone usługi push (ochrona przed SSRF) |
| `app.notifications.work-reminders.enabled` / `.cron` | `true` / `0 0 7 * * *` (czas polski) | codzienne przypomnienia o pracach na jutro |

Web Push wymaga HTTPS (albo `localhost`) i zbudowanej aplikacji (service worker nie działa w `npm run dev`).

---

## 🏗️ Architektura w skrócie

**Kontroler → fasada (`domain/<moduł>/facade/XxxFacade`) → handlery (`service/xxxfacade/*Handler`, jedna operacja =
jedna metoda `handle`)**. Reguły pilnuje `FacadeHandlerArchitectureTest`. Moduły komunikują się przez handlery
wyszukujące z kontrolą własności (`FindProjectOwnedByCurrentUserHandler`, `FindTreeInProjectOwnedByCurrentUserHandler`)
albo zdarzenia (`TreeMonitoringRecordSavedEvent` → powiadomienia) i punkty rozszerzeń (`UserAccountDeletionGuard`,
`UserAccountDataCleaner`). Opis modułów, przepływów (dokument, import/eksport, offline, Web Push) i decyzji:
[`docs/ARCHITEKTURA.md`](docs/ARCHITEKTURA.md).

```
domain/  user · project · tree · treephoto · treework · treevaluation · treemonitoring · treestand · treemarking
         referencelayer · mapsymbol · projectsummary · projectdocument · dataexchange (schema, imports, export, gis, crs) · notification
infrastructure/  api (kontrolery + obsługa błędów) · security · repository · storage · service (e-mail)
frontend/src/  api · auth · components · map · speech · gnss · offline · notifications
               pages/projects (summary, management, stands, dataexchange, documentation, integrations) · pages/trees (...)
```

---

## 🔌 REST API

Wszystko pod `/api`, poza rejestracją/logowaniem/resetem wymaga tokenu (`Authorization: Bearer …`). Pełny opis z
przykładami: Swagger UI `http://localhost:<port>/swagger-ui.html`. Brak tokenu → 401, zła rola → 403, cudzy albo
nieistniejący zasób → 404, błędy walidacji → 400 z mapą pól.

| Metoda | Ścieżka | Opis |
|---|---|---|
| POST | `/users/register`, `/users/login` | rejestracja, logowanie → `{ accessToken }` |
| GET / POST | `/users/confirm`, `/users/password-reset/*` | potwierdzenie e-mail, reset hasła |
| GET / PUT / DELETE | `/users/me` | własne konto (konto z projektami → 409) |
| GET | `/arborist/panel`, `/official/panel`, `/admin/*` | panele ról, administracja kontami (ADMIN) |
| GET / POST | `/projects` | własne projekty / nowy `{ name, description?, clientName?, siteAddress?, orderDate? }` |
| GET / PATCH | `/projects/{p}` | projekt; PATCH: pominięte pole = bez zmian, pusty tekst = usunięcie danych zlecenia |
| GET | `/projects/{p}/summary` | liczby projektu |
| GET / POST | `/projects/{p}/trees` | drzewa projektu / nowe drzewo |
| GET / PATCH | `/projects/{p}/trees/{t}` | drzewo / edycja (komplet pól) |
| PUT / DELETE | `/projects/{p}/trees/{t}/location` | lokalizacja WGS 84 (+ dokładność, wysokość, źródło) |
| PUT / DELETE | `/projects/{p}/trees/{t}/map-symbol`, `.../works/{w}/map-symbol` | symbole na mapie; katalog: `GET /map-symbols` |
| GET / POST / DELETE | `/projects/{p}/trees/{t}/photos[/{id}[/content]]` | zdjęcia (multipart `file`) |
| GET / PUT / DELETE | `/projects/{p}/trees/{t}/photos/{id}/annotation[/content]` | adnotacje zdjęć |
| GET | `/projects/{p}/photos` | zdjęcia projektu (dokumentacja) |
| GET / PUT / DELETE | `/projects/{p}/trees/{t}/valuation` | waloryzacja drzewa; `GET /projects/{p}/valuations` - całego projektu |
| GET / POST / PATCH / DELETE | `/projects/{p}/trees/{t}/works[/{w}]` | zalecenia i prace (status, priorytet, terminy) |
| GET | `/projects/{p}/works[?status=&priority=]` | prace całego projektu |
| GET / POST / PUT / DELETE | `/projects/{p}/trees/{t}/monitoring[/{r}]` | kontrole stanu drzewa (najnowsza pierwsza) |
| GET | `/projects/{p}/monitoring/latest` | ostatnia kontrola każdego drzewa |
| GET / POST / PUT / DELETE | `/projects/{p}/stands[/{s}]` | grupy drzew `{ name, notes?, geometry (GeoJSON Polygon), treeIds, assignTreesInside? }` |
| POST | `/projects/{p}/stands/{s}/assign-trees-inside` | przypisz drzewa leżące w obszarze |
| GET | `/projects/{p}/exports/trees.csv` · `.xlsx` · `.geojson` · `.gpkg` | eksport drzew (GPKG: warstwy `trees` i `stands`) |
| GET | `/projects/{p}/exports/works.csv` · `stands.geojson` · `shapefile.zip` · `calendar.ics` | prace, grupy drzew, Shapefile, kalendarz prac |
| POST | `/projects/{p}/imports/trees[/validate]` | import drzew: CSV, XLSX, GeoJSON, `.zip` (Shapefile), `.gpkg`, `.dxf` |
| POST | `/projects/{p}/imports/stands[/validate]` | import grup drzew (obszarów): DXF, Shapefile ZIP, GeoJSON, GeoPackage |
| GET | `/projects/{p}/documents/project-report` | „Dokument projektu” PDF |
| GET | `/notifications` | ostatnie powiadomienia zalogowanego |
| GET | `/notifications/push/config` | klucz publiczny VAPID |
| PUT · POST | `/notifications/push/subscription` · `.../removal` · `.../status` | Web Push na tym urządzeniu: włącz / wyłącz / stan |
| POST | `/notifications/test` | powiadomienie testowe → `{ deliveredDeviceCount }` |

Import zawsze „wszystko albo nic” (najpierw `validate`, błędy → 400 z wynikiem), każdy obiekt pliku = nowe drzewo /
nowa grupa. Identyfikatory projektu, drzewa i właściciela nigdy nie pochodzą z pliku ani z treści żądania.

---

## 🧪 Testy

```bash
./mvnw test                 # backend: testy jednostkowe i integracyjne (H2, prawdziwy SecurityConfig i JWT)
cd frontend && npm test     # frontend: testy logiki (node --test)
npm run build               # tsc + build do src/main/resources/static
```

Najważniejsze zestawy: `*ApiIntegrationTest` dla każdego modułu (CRUD, walidacja, **IDOR** - użytkownik B na danych A),
`MilestoneOwnershipRegressionIntegrationTest`, `SecurityRulesTest`, `FacadeHandlerArchitectureTest`,
`GisImportApiIntegrationTest` (DXF w PL-2000, arkusz z aplikacji terenowej, pętle eksport → import SHP / GPKG),
`WebPushCryptographyTest` (wektor z RFC 8291, VAPID), `NotificationApiIntegrationTest` (fałszywa usługa push, SSRF),
`PolishGridCrsTest` (wzorce PROJ), testy offline (`offlineQueue.test.ts`). Testy E2E w przeglądarce (headless Chrome)
były uruchamiane przy każdym etapie poza repozytorium - opis wyników w raportach etapów.

---

## 🗄️ Schemat bazy

Brak Flyway/Liquibase - schemat tworzy i rozszerza Hibernate (`ddl-auto=update`) w schemacie `arbomachine`; wszystkie
nowe kolumny są opcjonalne, więc aktualizacja nie traci danych. Tabele (bez `owner_id` poza `project`):

| Tabela | Zawartość |
|---|---|
| `user_account`, `email_confirmation_token`, `password_reset_token` | konta i tokeny |
| `project` | `owner_id`, `name`, `description`, `client_name`, `site_address`, `order_date`, daty |
| `tree` | `project_id`, numer, gatunki, pomiary (`trunk_circumference_cm`, `height_m`, `crown_diameter_m`), opisy, `map_symbol_code`, lokalizacja (`latitude`, `longitude`, `location_accuracy_m`, `location_altitude_m`, `location_source`) |
| `tree_photo`, `tree_photo_annotation` | metadane zdjęć (pliki na dysku), adnotacje (JSON + PNG) |
| `tree_work` | prace: `name`, `description`, `map_symbol_code`, `status`, `priority`, `planned_date`, `completed_date` |
| `tree_valuation` | waloryzacja 1:1 z drzewem: `overall_category` + opisy |
| `tree_monitoring_record` | kontrole: `inspection_date`, `health_status`, `notes` (indeks `tree_id, inspection_date`) |
| `tree_stand`, `tree_stand_tree` | grupy drzew: `geometry_geojson` (Polygon WGS 84), `area_square_meters`, `source`; przypisane drzewa |
| `user_notification`, `web_push_subscription`, `web_push_server_key` | powiadomienia (z kluczem deduplikacji), subskrypcje push, klucze VAPID |

---

## 🧭 Trasy frontendu (arborysta)

| Trasa | Zawartość |
|---|---|
| `/projects` | lista projektów, „Nowy projekt” |
| `/projects/:id` | podsumowanie projektu |
| `/projects/:id/trees` · `/trees/:treeId` | lista drzew z filtrami (waloryzacja, stan, grupa, iglaste/liściaste) · karta drzewa |
| `/projects/:id/stands` | grupy drzew (rysowanie obszaru, przypisywanie drzew) |
| `/projects/:id/map` | mapa: tryby, grupy drzew, legendy |
| `/projects/:id/management` | gospodarka drzewostanem |
| `/projects/:id/documentation` | dokument PDF i materiały zdjęciowe |
| `/projects/:id/import-export` | import drzew i grup drzew, eksporty |
| `/projects/:id/integrations` | kalendarz ICS, powiadomienia push, ostatnie powiadomienia |
| `/arborist/profile` | profil, powiadomienia na tym urządzeniu |

---

## 📚 Dokumentacja

- [`docs/DO_DOPRECYZOWANIA.md`](docs/DO_DOPRECYZOWANIA.md) - pytania do arborystów, decyzje V1, rejestr braków i długu
- [`docs/ARCHITEKTURA.md`](docs/ARCHITEKTURA.md) - moduły, przepływy, bezpieczeństwo
- [`docs/WYDANIE.md`](docs/WYDANIE.md) - build, uruchomienie, wydanie, kopie zapasowe, wycofanie wersji
- [`docs/integrations/qgis.md`](docs/integrations/qgis.md) - wymiana danych z QGIS
- [`docs/discovery/`](docs/discovery) - discovery i decyzje poszczególnych etapów

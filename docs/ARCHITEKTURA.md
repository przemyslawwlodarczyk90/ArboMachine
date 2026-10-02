# Architektura ArboMachine (stan po etapie 30)

Jeden proces Spring Boot (Java 25) serwuje REST API (`/api/**`) i zbudowaną aplikację React (SPA). Baza PostgreSQL
(schemat `arbomachine`, tabele tworzy Hibernate), pliki zdjęć na dysku serwera.

## Warstwy backendu

```
infrastructure/api/<moduł>/XxxController      HTTP, walidacja DTO, mapowanie wyjątków (XxxExceptionHandler)
        │
domain/<moduł>/facade/XxxFacade                jedyne wejście do modułu z zewnątrz
        │
domain/<moduł>/service/xxxfacade/*Handler      jedna operacja = jedna klasa z metodą handle(...)
        │
infrastructure/repository/*Repository          Spring Data JPA
```

- `FacadeHandlerArchitectureTest` pilnuje, że w `service/` są tylko katalogi `*facade` z handlerami. Kod
  niebędący operacją fasady (zadania cykliczne, nasłuchiwanie zdarzeń, kryptografia, czytniki plików) leży w
  osobnych pakietach modułu: `delivery/`, `reminder/`, `push/`, `gis/`, `crs/`, `geometry/`, `assembler/`,
  `generator/`, `export/`, `imports/`.
- **Własność danych:** identyfikatory projektu i drzewa przychodzą z URL, użytkownik z JWT; każdy dostęp idzie przez
  `FindProjectOwnedByCurrentUserHandler` / `FindTreeInProjectOwnedByCurrentUserHandler`. Cudzy zasób = 404 (nie 403),
  by nie zdradzać istnienia. Testy IDOR w każdym `*ApiIntegrationTest`.
- **Granice modułów:** moduł nie sięga do encji innego modułu poza „własnością” (Project → Tree). Komunikacja:
  - zdarzenia Springa po zatwierdzeniu transakcji: `TreeMonitoringRecordSavedEvent` → moduł `notification`;
  - punkty rozszerzeń modułu `user`: `UserAccountDeletionGuard` (np. konto z projektami → 409),
    `UserAccountDataCleaner` (usuwanie subskrypcji i powiadomień razem z kontem);
  - dane do eksportu i dokumentu zbierają asemblery do własnych rekordów (`ProjectExportData`,
    `ProjectDocumentData`), więc generatory nie znają encji JPA.

## Moduły domenowe

| Moduł | Odpowiedzialność |
|---|---|
| `user` | konta, role (ARBORIST / OFFICIAL / ADMIN), rejestracja z e-mailem, reset hasła |
| `project` | projekt arborysty, dane zlecenia |
| `tree` | drzewo: pomiary, opisy, lokalizacja (WGS 84 + metadane GNSS) |
| `treephoto` | zdjęcia (pliki na dysku), adnotacje (JSON + PNG) |
| `treework` | zalecenia i prace: status, priorytet, terminy |
| `treevaluation` | waloryzacja — zapis oceny arborysty |
| `treemonitoring` | kontrole stanu drzewa |
| `treestand` | grupy drzew: wielokąt (GeoJSON), pole, przypisane drzewa (`StandPolygon`: walidacja, pole, punkt-w-wielokącie) |
| `mapsymbol` | katalog symboli mapy |
| `projectsummary` | liczby projektu |
| `projectdocument` | „Dokument projektu” PDF |
| `dataexchange` | import / eksport: `schema` (pola wymiany), `imports`, `export`, `gis` (SHP, GPKG, DXF, GeoJSON), `crs` (PL-2000, PL-1992) |
| `notification` | powiadomienia, Web Push, przypomnienia |

## Przepływy

**Dokument PDF:** `ProjectDocumentDataAssembler` (dane projektu, drzew, zdjęć, prac, waloryzacji, kontroli, grup →
rekordy tylko do odczytu, sortowanie naturalne) → `ProjectReportPdfGenerator` (szablon Thymeleaf
`templates/documents/project-report.html` → openhtmltopdf). Generowanie synchroniczne, cały plik w pamięci.

**Eksport:** `ProjectExportDataAssembler` → exportery (`TreeCsvExporter`, `TreeXlsxExporter`, `TreeGeoJsonExporter`,
`TreeGeoPackageExporter`, `ShapefileZipExporter`, `TreeStandsGeoJsonExporter`, `TreeWorksCsvExporter`,
`TreeWorksIcsExporter`). Lista pól jest w jednym miejscu: `TreeExchangeField` (nagłówek CSV/XLSX, nazwa GeoJSON,
nazwa pola DBF ≤ 10 znaków).

**Import:** plik → parser formatu (`*TreeImportParser`, czytniki `gis/`) → wiersze → przeliczenie układu
(`PolishGridCrs`) → `TreeImportAnalyzer` / `TreeStandImportAnalyzer` (walidacja, ostrzeżenia, podgląd) → zapis
„wszystko albo nic” w jednej transakcji. Ten sam analizator obsługuje `validate` i właściwy import.

**Powiadomienia:** zdarzenie / zadanie cykliczne → `UserNotificationRecorder` (osobna transakcja, deduplikacja po
kluczu) → `NotificationDispatcher` → pula wątków `notificationDeliveryExecutor` → `WebPushDelivery` (szyfrowanie
RFC 8291, VAPID, lista dozwolonych hostów; 404/410 usuwa subskrypcję).

**Offline (frontend):** `offline/` — kopie odczytów i kolejka zmian w IndexedDB, wysyłka FIFO istniejącymi
endpointami po powrocie sieci, tymczasowe ujemne id z mapowaniem po synchronizacji. Service worker
(`public/sw.js`) — powłoka aplikacji i obsługa Web Push. Grupy drzew i zdjęcia nie działają offline.

**GPS:** `gnss/deviceGnss.ts` — Geolocation API przeglądarki (dokładność, wysokość, źródło zapisane z punktem).
Odbiorniki RTK / NTRIP — niezaimplementowane (OPEN).

## Frontend

React 19 + TypeScript + Vite, bez biblioteki stanu (hooki + moduły API per obszar). OpenLayers dla map. Logika
testowana `node --test` (pliki `*.test.ts`); komponenty React bez testów renderowania (DŁUG). Błędy API
tłumaczone na polski w `userFacingErrorMessages.ts`.

## Decyzje techniczne

- **Bez PostGIS:** geometria jako GeoJSON w kolumnie tekstowej, obliczenia w Javie — wystarcza dla setek obiektów
  w projekcie, nie wymaga rozszerzeń bazy.
- **Bez Flyway:** `ddl-auto=update`, wszystkie nowe kolumny opcjonalne (dług opisany w rejestrze braków).
- **Formaty GIS bez GDAL:** Shapefile, DBF, DXF, układy PL-2000/PL-1992 i Web Push — własny kod na bibliotece
  standardowej Javy (brak natywnych zależności poza SQLite w GeoPackage).
- **Pojedyncza instancja:** zadanie przypomnień i kolejka wysyłki działają w procesie (skalowanie = rejestr braków).

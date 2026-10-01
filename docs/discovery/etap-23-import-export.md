# Etap 23 — import / eksport danych

**Status:** V1 zaimplementowane. Formaty zewnętrzne są wyłącznie adapterami wejścia/wyjścia — model domenowy
`Project → Tree (→ TreeLocation, TreeWork, TreePhoto)` nie zależy od żadnego z nich. Niepotwierdzone decyzje
są w `docs/DO_DOPRECYZOWANIA.md` (sekcja „Import i eksport”).

## Formaty V1

| Format | Eksport | Import | Uwagi |
|---|---|---|---|
| CSV | tak | tak | separator `;`, UTF-8 (eksport z BOM), przecinek dziesiętny |
| XLSX | tak | tak | arkusze „Drzewa” i „Projekt”; liczby jako liczby; `.xls` nieobsługiwany |
| GeoJSON | tak | tak | FeatureCollection / Point / WGS 84 — patrz etap 24 i `docs/integrations/qgis.md` |
| GeoPackage | tak (etap 24) | nie | warstwa `trees`, POINT, EPSG:4326 |

Endpointy (wszystkie wymagają JWT i sprawdzają własność projektu — cudzy projekt → 404):

| Metoda | Ścieżka | Wynik |
|---|---|---|
| GET | `/api/projects/{projectId}/exports/trees.csv` | `text/csv; charset=UTF-8` |
| GET | `/api/projects/{projectId}/exports/trees.xlsx` | XLSX |
| GET | `/api/projects/{projectId}/exports/trees.geojson` | `application/geo+json` |
| GET | `/api/projects/{projectId}/exports/trees.gpkg` | `application/geopackage+sqlite3` |
| POST | `/api/projects/{projectId}/imports/trees/validate` | multipart `file` → wynik sprawdzenia (nic nie zapisuje) |
| POST | `/api/projects/{projectId}/imports/trees` | multipart `file` → 201 `{importedTreeCount, warningCount}` albo 400 z wynikiem sprawdzenia |

Nazwa pobieranego pliku: `arbomachine-projekt-{id}-drzewa.{csv|xlsx|geojson|gpkg}` (bez nazwy projektu).

## Jedno źródło mapowania pól

`domain/dataexchange/schema/TreeExchangeField` — **jedyne** miejsce łączące pole techniczne, krótką nazwę GIS
i polski nagłówek („ArboMachine Tree Exchange Schema V1”, `SCHEMA_VERSION = "1"`):

| Pole techniczne (GeoJSON, API) | Nazwa GIS (GeoPackage) | Nagłówek CSV/XLSX | Typ | Import |
|---|---|---|---|---|
| `treeId` | `tree_id` | ID drzewa (ArboMachine) | liczba całkowita | **ignorowane** |
| `inventoryNumber` | `inventory_no` | Numer inwentaryzacyjny | tekst | **wymagane** |
| `speciesPolish` | `species_pl` | Gatunek polski | tekst | tak |
| `speciesLatin` | `species_la` | Gatunek łaciński | tekst | tak |
| `trunkCircumferenceCm` | `circum_cm` | Obwód pnia [cm] | liczba całkowita | tak |
| `heightM` | `height_m` | Wysokość [m] | liczba | tak |
| `crownDiameterM` | `crown_m` | Średnica korony [m] | liczba | tak |
| `notes` | `notes` | Notatki | tekst | tak |
| `conditionNotes` | `condition` | Stan drzewa | tekst | tak |
| `hazardNotes` | `hazards` | Zagrożenia | tekst | tak |
| `latitude` | (geometria) | Szerokość geograficzna | liczba (WGS 84) | tak |
| `longitude` | (geometria) | Długość geograficzna | liczba (WGS 84) | tak |
| `mapSymbolCode` | `map_symbol` | — (tylko GIS) | tekst | ignorowane |
| `workNames` | `work_names` | Zalecenia i prace | tekst (nazwy rozdzielone „; ”) | ignorowane |
| `locationAccuracyM` | `accuracy_m` | Dokładność lokalizacji [m] | liczba (etap 25) | ignorowane |
| `locationAltitudeM` | `altitude_m` | Wysokość n.p.m. [m] | liczba (etap 25) | ignorowane |
| `locationSource` | `loc_source` | Źródło lokalizacji | `MANUAL`/`MAP`/`DEVICE_GNSS`/`IMPORT` (etap 25) | ignorowane (import ustawia `IMPORT`) |
| `valuationCategory` | `val_categ` | Waloryzacja - ocena ogólna | `LOW`/`MEDIUM`/`HIGH`/`VERY_HIGH` (etap 27) | ignorowane |
| `valuationAgeValue` … `valuationJustification` | `val_age`, `val_nature`, `val_ecol`, `val_cult`, `val_landsc`, `val_justif` | Wartość wiekowa / przyrodnicza / ekologiczna / kulturowa / krajobrazowa, Uzasadnienie waloryzacji | tekst - tylko CSV/XLSX (nie GIS) | ignorowane |

Przy imporcie nagłówek kolumny może być polską etykietą, nazwą techniczną albo nazwą GIS (wielkość liter
bez znaczenia) — plik wyeksportowany z ArboMachine albo z QGIS wraca bez zmian nagłówków.

## Schema CSV

- Kodowanie UTF-8; eksport dodaje BOM (Excel poprawnie pokazuje polskie znaki). Import wymaga UTF-8
  (z BOM albo bez) — inne kodowanie (np. Windows-1250) → błąd „zapisz jako CSV UTF-8”.
- Separator eksportu `;` (polski Excel używa przecinka jako separatora dziesiętnego). Import wykrywa separator
  z wiersza nagłówka: `;`, `,` albo tabulator.
- Liczby w eksporcie z przecinkiem dziesiętnym (`18,5`) i bez zbędnych zer; import przyjmuje przecinek albo kropkę,
  bez separatorów tysięcy.
- Brak wartości = puste pole. Escaping (cudzysłowy, separator i nowe linie w notatkach) — Apache Commons CSV.
- Kolejność kolumn eksportu: jak w tabeli wyżej (bez `mapSymbolCode`); końce linii CRLF.

## Schema XLSX

- Arkusz **Drzewa**: wiersz 1 — polskie nagłówki (pogrubione, zamrożone), kolumny jak w CSV; liczby jako komórki
  liczbowe; puste komórki dla braków; podstawowe szerokości kolumn.
- Arkusz **Projekt**: nazwa, opis, data eksportu, układ współrzędnych, wersja schematu, liczba drzew,
  liczba drzew bez lokalizacji.
- Import: arkusz „Drzewa”, a gdy go nie ma — pierwszy arkusz; pierwszy niepusty wiersz = nagłówki.
- Biblioteka: fastexcel (zapis i strumieniowy odczyt) — lżejsza niż Apache POI.

## Properties GeoJSON

FeatureCollection; jedno drzewo z lokalizacją = jeden Feature (`id` = treeId) z geometrią
`Point [długość, szerokość]`. Properties: nazwy techniczne z tabeli (bez `latitude/longitude` — są w geometrii),
braki jako `null`, dodatkowo `works: [{name, description, mapSymbolCode}]`. Człon `arboMachine`
(dozwolony „foreign member” RFC 7946): `schemaVersion`, `crs`, `projectId`, `exportedAt`, `treeCount`,
`treesWithoutLocationCount`. Zdjęć nie eksportujemy (żadnego Base64).

## CRS

V1: **EPSG:4326 / WGS 84** — ten sam układ co `TreeLocation`. Domena się nie zmienia; miejsce na przyszłą
transformację (np. PL-2000, PL-1992) to interfejs `CoordinateTransformer` (V1: `Wgs84CoordinateTransformer`,
bez zmiany wartości). Import GeoJSON akceptuje brak członu `crs` albo CRS84 / EPSG:4326; inny → błąd pliku.

## Import flow

1. Użytkownik wybiera plik (CSV, XLSX, GeoJSON) w zakładce „Import i eksport”.
2. „Sprawdź plik” → backend parsuje i waliduje, **nic nie zapisuje**: liczba wierszy, gotowe do importu, błędy
   (z numerem wiersza i kolumną z pliku), ostrzeżenia, pominięte kolumny, podgląd pierwszych 10 wierszy.
3. „Importuj drzewa” (aktywne tylko bez błędów) → backend sprawdza plik **ponownie** (bez stanu na serwerze —
   plik jest wysyłany drugi raz) i zapisuje wszystkie drzewa w **jednej transakcji**.
4. Jakikolwiek błąd → 400, nic nie zostaje zapisane („wszystko albo nic”); awaria bazy w trakcie → wycofanie całości.

## Walidacja

Te same zasady co formularz drzewa: numer wymagany (do 100 znaków), gatunki do 255 znaków, obwód — liczba
całkowita 1–5000 cm, wysokość (0, 150] m i średnica korony (0, 200] m z maks. 2 miejscami po przecinku,
szerokość −90…90, długość −180…180, lokalizacja kompletna albo pusta. Puste wiersze są pomijane.
Limity techniczne: plik do 5 MB, do 2000 drzew w jednym imporcie.

API zwraca **kody** problemów (`REQUIRED_VALUE_MISSING`, `INVALID_NUMBER`, `VALUE_OUT_OF_RANGE`…), a polskie zdania
(„Wiersz 42: nieprawidłowa liczba w kolumnie „Wysokość [m]” („dużo”).”) składa frontend
(`importIssueMessages.ts`). Użytkownik nigdy nie widzi nazw wyjątków Javy.

## Duplikaty

V1: **każdy wiersz tworzy nowe drzewo**, także gdy numer już istnieje w projekcie albo powtarza się w pliku —
zasady unikalności numerów nie są ustalone, więc nie wprowadzamy ich tylko dla importu. Duplikaty są pokazywane
jako **ostrzeżenia** (nie blokują importu). Import nigdy nie aktualizuje istniejących drzew.

## Identyfikatory

Import nie przyjmuje z pliku id bazy, projektu ani właściciela: kolumna / właściwość `treeId` jest ignorowana
(widoczna jako „pominięta kolumna”), projekt pochodzi z URL, użytkownik z JWT. Zmanipulowany `treeId` cudzego
drzewa nie ma żadnego wpływu (test integracyjny).

## Ograniczenia V1

- Import tylko podstawowych danych drzewa (bez prac, symboli, zdjęć).
- Brak aktualizacji istniejących drzew i scalania; brak „importuj poprawne wiersze mimo błędów”.
- Brak mapowania kolumn w UI (nagłówki muszą odpowiadać schematowi).
- Eksport zawsze w EPSG:4326; brak wyboru układu.
- `.xls` (stary Excel) nieobsługiwany; CSV tylko w UTF-8.

## Formaty świadomie odłożone

SHP, DXF, DWG, QGZ, styl QML, WFS/WMS, formaty konkretnych urzędów i klientów, import zdjęć (ZIP), import całego
projektu, synchronizacja dwukierunkowa, import GeoPackage. Uzasadnienie i pytania: `docs/DO_DOPRECYZOWANIA.md`.

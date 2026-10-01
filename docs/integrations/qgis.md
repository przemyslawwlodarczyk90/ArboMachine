# QGIS

ArboMachine nie zastępuje QGIS — przekazuje do niego dane drzew i potrafi przyjąć je z powrotem jako nowe drzewa.

## Eksport z ArboMachine

1. Otwórz projekt.
2. Przejdź do zakładki **„Import i eksport”**.
3. W bloku **QGIS** kliknij **„Pobierz GeoPackage”** (zalecane) albo **„Pobierz GeoJSON”**.
4. Uruchom QGIS.
5. Wybierz **Warstwa → Dodaj warstwę → Dodaj warstwę wektorową** (albo przeciągnij plik na mapę).
6. Wskaż pobrany plik. Dla GeoPackage wybierz warstwę **trees**.
7. Otwórz **tabelę atrybutów** (prawy przycisk na warstwie), aby zobaczyć dane drzew.

Drzewa bez lokalizacji nie trafiają do plików GIS (zakładka pokazuje ich liczbę przed pobraniem) — są w CSV/XLSX.

## CRS

**EPSG:4326 (WGS 84)** — ten sam układ, w którym ArboMachine przechowuje punkty drzew. QGIS przelicza warstwę
„w locie” na układ projektu mapy (np. PL-1992 / EPSG:2180). Eksport w innych układach — jeszcze nie (V1).

## Warstwa

| | |
|---|---|
| Nazwa warstwy | `trees` |
| Geometria | Point (jedno drzewo = jeden punkt) |
| GeoPackage | plik `.gpkg`, tabela `trees`, kolumna geometrii `geom` |
| GeoJSON | FeatureCollection; współrzędne `[długość, szerokość]` |

## Atrybuty

GeoPackage używa krótkich nazw pól („QGIS compatibility profile V1”), GeoJSON — nazw technicznych
(„ArboMachine GeoJSON Schema V1”). Nazwy są stabilne — można na nich opierać style i etykiety w QGIS.

| GeoPackage | GeoJSON | Znaczenie |
|---|---|---|
| `tree_id` | `treeId` | ID drzewa w ArboMachine (tylko informacyjnie) |
| `inventory_no` | `inventoryNumber` | numer inwentaryzacyjny |
| `species_pl` | `speciesPolish` | gatunek — nazwa polska |
| `species_la` | `speciesLatin` | gatunek — nazwa łacińska |
| `circum_cm` | `trunkCircumferenceCm` | obwód pnia [cm] |
| `height_m` | `heightM` | wysokość [m] |
| `crown_m` | `crownDiameterM` | średnica korony [m] |
| `notes` | `notes` | notatki |
| `condition` | `conditionNotes` | stan drzewa (opis) |
| `hazards` | `hazardNotes` | zagrożenia (opis) |
| `map_symbol` | `mapSymbolCode` | kod symbolu drzewa z mapy ArboMachine (np. `GREEN_CIRCLE`) |
| `work_names` | `workNames` | nazwy zaleceń / prac rozdzielone „; ” |
| — | `works` | (tylko GeoJSON) lista prac: nazwa, opis, kod symbolu |

Puste pole = brak danych. Kody symboli to dane, a nie styl — QGIS może po nich stylować warstwę
(np. styl kategoryzowany po `map_symbol`).

## Powrót z QGIS do ArboMachine

1. W QGIS: prawy przycisk na warstwie → **Eksportuj → Zapisz obiekty jako…** → format **GeoJSON**,
   układ **EPSG:4326**.
2. W ArboMachine: **Import i eksport → Import drzew** → wybierz plik → **„Sprawdź plik”** → **„Importuj drzewa”**.

Import **tworzy nowe drzewa** — nie aktualizuje istniejących (także wtedy, gdy plik ma pole `tree_id`, które
jest ignorowane). Jeżeli numery drzew już są w projekcie, zobaczysz ostrzeżenie. Import przyjmuje GeoJSON
z punktami w WGS 84; plik w innym układzie (np. PL-1992) zostanie odrzucony z komunikatem.

## Ograniczenia V1

- brak gotowego projektu QGIS (`.qgz`), stylu (`.qml`), etykiet i układu wydruku,
- brak synchronizacji z QGIS — tylko pliki,
- brak aktualizacji istniejących drzew przy imporcie (round-trip = nowe drzewa),
- tylko EPSG:4326 (bez PL-2000 / PL-1992 przy eksporcie),
- brak SHP, DXF i formatów konkretnych urzędów — **nie ma gwarancji zgodności z formatami urzędowymi**,
- import GeoPackage — jeszcze nie (import przez GeoJSON),
- zdjęcia i adnotacje nie są eksportowane.

## Test akceptacyjny (ręczny, w prawdziwym QGIS)

Automatycznie sprawdzono pliki biblioteką GDAL 3.12 (silnik odczytu wektorów w QGIS) — patrz
`docs/discovery/etap-24-qgis.md`. **W samym QGIS test nie był jeszcze wykonany.** Checklista:

- [ ] utwórz projekt
- [ ] dodaj 5 drzew
- [ ] dodaj lokalizację do 4
- [ ] dodaj gatunki i pomiary
- [ ] dodaj zalecenie / pracę
- [ ] eksportuj GeoJSON i GeoPackage
- [ ] otwórz QGIS i dodaj warstwę
- [ ] sprawdź liczbę punktów (4)
- [ ] sprawdź położenie (na podkładzie OSM)
- [ ] otwórz tabelę atrybutów
- [ ] sprawdź polskie znaki
- [ ] sprawdź pomiary (liczby, nie tekst)
- [ ] sprawdź `tree_id` / `inventory_no`
- [ ] przesuń testowo punkt
- [ ] eksportuj z QGIS do GeoJSON (EPSG:4326)
- [ ] zaimportuj plik do ArboMachine („Sprawdź plik” → „Importuj drzewa”) — powstają NOWE drzewa
- [ ] aktualizacja istniejących drzew z QGIS — **NOT SUPPORTED IN V1**

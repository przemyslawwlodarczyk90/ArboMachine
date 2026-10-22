# QGIS

ArboMachine nie zastępuje QGIS — przekazuje do niego dane drzew i potrafi przyjąć je z powrotem jako nowe drzewa.

## Eksport z ArboMachine

1. Otwórz projekt.
2. Przejdź do zakładki **„Import i eksport”**.
3. W bloku **QGIS** wybierz **układ współrzędnych** (domyślnie PL-2000 ze strefą dobraną do drzew) i kliknij
   **„Pobierz pakiet dla QGIS (ZIP)”** (zalecane, etap 34), **„Pobierz GeoPackage”** albo **„Pobierz GeoJSON”** (WGS 84).
4. Rozpakuj pakiet (instrukcja także w `README.txt` w pakiecie) i uruchom QGIS.
5. Przeciągnij plik `.gpkg` na mapę (albo **Warstwa → Dodaj warstwę → Dodaj warstwę wektorową**) i zaznacz warstwy
   **trees**, **tree_markings** i **stands**.
6. Style wczytują się same z tabeli `layer_styles`. Jeśli nie: **Właściwości warstwy → Styl → Wczytaj styl** i plik
   `.qml` o nazwie warstwy.
7. Otwórz **tabelę atrybutów** (prawy przycisk na warstwie), aby zobaczyć dane drzew.

Drzewa bez lokalizacji nie trafiają do plików GIS (zakładka pokazuje ich liczbę przed pobraniem) — są w CSV/XLSX.

## CRS

ArboMachine przechowuje punkty w **EPSG:4326 (WGS 84)**. Od etapu 34 pakiet dla QGIS i GeoPackage można pobrać
w **PL-2000** (EPSG:2176-2179; „PL-2000” = strefa, w której leży najwięcej drzew), **PL-1992** (EPSG:2180) albo WGS 84 —
wszystkie warstwy pliku w jednym układzie, współrzędne x = wschód, y = północ w metrach (dokładność 1 mm, to samo
odwzorowanie co import, zgodne z PROJ). GeoJSON i Shapefile są zawsze w EPSG:4326. Parametr API: `?crs=`.

## Warstwa

| | |
|---|---|
| Nazwa warstwy | `trees` (+ od etapu 34 `tree_markings`: jedno oznaczenie = jeden punkt drzewa, i `stands`: grupy drzew) |
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
| `accuracy_m` | `locationAccuracyM` | dokładność lokalizacji [m] (etap 25; puste = nieznana) |
| `altitude_m` | `locationAltitudeM` | wysokość n.p.m. [m] z GNSS (etap 25) |
| `loc_source` | `locationSource` | źródło punktu: `MANUAL`, `MAP`, `DEVICE_GNSS`, `IMPORT` (puste = sprzed etapu 25) |
| `val_categ` | `valuationCategory` | waloryzacja - ocena ogólna V1: `LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH` (puste = nie oceniono; to nie skala branżowa, etap 27) |
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

- brak gotowego projektu QGIS (`.qgz`) i układu wydruku (style `.qml` i etykiety numerów drzew — od etapu 34),
- brak synchronizacji z QGIS — tylko pliki,
- brak aktualizacji istniejących drzew przy imporcie (round-trip = nowe drzewa),
- GeoJSON i Shapefile tylko w EPSG:4326 (PL-2000 / PL-1992 — w GeoPackage i pakiecie QGIS),
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

Etap 34 (pakiet dla QGIS) — sprawdzone automatycznie: odczyt biblioteką GeoPackage w testach i GDAL 3.10.3
(`ogrinfo`: warstwy, układ EPSG:2178, style w `layer_styles`). W QGIS do sprawdzenia:

- [ ] pakiet w PL-2000: układ rozpoznany automatycznie, punkty pokrywają się z ortofotomapą Geoportalu
- [ ] style wczytują się same (trees: numery, tree_markings: kolorowe znaczki wokół drzewa, stands: obszary)
- [ ] drzewo z 3 oznaczeniami pokazuje 3 znaczki obok siebie
- [ ] ręczne wczytanie `.qml` daje ten sam wygląd
- [ ] to samo w QGIS 3.44.x, 4.2.x i wersji klienta

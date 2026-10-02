# Etap 24 — QGIS

# Cel

Zbudować działający most **ArboMachine ↔ QGIS** na plikach: drzewa projektu jako punkty z atrybutami w QGIS
i możliwość wczytania danych z QGIS z powrotem. Nie budujemy GIS w ArboMachine (bez warstw, geoprocessingu,
reprojekcji w UI, stylowania i układów wydruku) — integrujemy się z QGIS, nie konkurujemy z nim.

# Co wiemy o obecnym workflow arborysty

Z dotychczasowych materiałów: arborysta pracuje w aplikacji terenowej (symbole, rysunki, DXF/PDF, eksport GIS), ZWCAD
i QGIS; dane tabelaryczne trzyma w XLS. Nie wiemy, co dokładnie trafia do QGIS, w jakim formacie i układzie,
co jest tam poprawiane i czy dane wracają (pytania niżej).

# Co ArboMachine obsługuje obecnie

- Eksport drzew: CSV, XLSX, **GeoJSON**, **GeoPackage** (etap 23–24).
- Import drzew: CSV, XLSX, **GeoJSON** (najpierw „Sprawdź plik”, potem import „wszystko albo nic”).
- Jeden schemat pól dla wszystkich formatów: `TreeExchangeField` (`docs/discovery/etap-23-import-export.md`).
- Miejsce na transformację CRS: `CoordinateTransformer` (V1: EPSG:4326 bez zmian).

# Workflow ArboMachine -> QGIS

Zakładka „Import i eksport” → blok QGIS → „Pobierz GeoPackage” / „Pobierz GeoJSON” → w QGIS „Dodaj warstwę
wektorową” → warstwa `trees` (Point, EPSG:4326) z atrybutami. Instrukcja: `docs/integrations/qgis.md`.

# Workflow QGIS -> ArboMachine

QGIS: „Zapisz obiekty jako…” → GeoJSON w EPSG:4326 → ArboMachine: „Import drzew” → „Sprawdź plik” → „Importuj
drzewa”. **V1: import tworzy NOWE drzewa.** `tree_id` / `treeId` z pliku jest ignorowany — plik to dane
zewnętrzne, a aktualizacja po id wymagałaby świadomej strategii identyfikacji (patrz pytania). Ownership:
projekt z URL i użytkownik z JWT; zmanipulowane id cudzego drzewa nie ma wpływu (test automatyczny).

# Format V1

- **GeoPackage** („QGIS compatibility profile V1”): plik `.gpkg`, warstwa `trees`, POINT, EPSG:4326, krótkie
  nazwy pól (`tree_id`, `inventory_no`, `species_pl`, `species_la`, `circum_cm`, `height_m`, `crown_m`, `notes`,
  `condition`, `hazards`, `map_symbol`, `work_names`). Biblioteka NGA GeoPackage (czysta Java + sqlite-jdbc
  z natywnym SQLite w jarze, ~16 MB zależności, bez GDAL). Ocena: rozsądny koszt → zaimplementowane.
- **GeoJSON** („ArboMachine GeoJSON Schema V1”): FeatureCollection `name: "trees"`, Point `[długość, szerokość]`,
  nazwy techniczne camelCase, człon `arboMachine.schemaVersion = "1"`.
- Wersja schematu: `TreeExchangeField.SCHEMA_VERSION = "1"` (GeoJSON: człon `arboMachine`, XLSX: arkusz
  „Projekt”, GeoPackage: opis warstwy w `gpkg_contents`).

# CRS V1

EPSG:4326 / WGS 84 — układ `TreeLocation`. Domena się nie zmienia; transformacje należą do warstwy
import/eksport (`CoordinateTransformer`). Import GeoJSON przyjmuje brak `crs`, CRS84 albo EPSG:4326;
inny układ (np. EPSG:2180) → czytelny błąd pliku. Docelowe układy (PL-2000 strefa?, PL-1992) — OPEN.

# Mapping pól

Pełna tabela: `docs/discovery/etap-23-import-export.md` („Jedno źródło mapowania pól”) i `docs/integrations/qgis.md`.
Symbole mapowe eksportujemy jako dane (`map_symbol`, `works[].mapSymbolCode`) — nie jako nazwy ikon OpenLayers
i nie jako standard GIS; QGIS może po nich stylować.

# Ograniczenia

Brak QGZ, QML, etykiet, układu wydruku, podkładów; brak synchronizacji; round-trip tylko jako nowe drzewa;
tylko EPSG:4326; brak SHP / DXF / formatów urzędowych; brak importu GeoPackage; bez zdjęć.

# Rzeczy przetestowane

- Testy automatyczne (JUnit, H2): schema eksportu GIS, Point, kolejność `[długość, szerokość]`, properties,
  założenie CRS (brak `crs` w GeoJSON, `arboMachine.crs = EPSG:4326`, SRS 4326 w GeoPackage), pomijanie drzew
  bez lokalizacji, polskie znaki, ownership (eksport/sprawdzenie/import cudzego projektu → 404), zmanipulowany
  `treeId`, nieprawidłowy GeoJSON, geometria inna niż Point, CRS EPSG:2180 odrzucony, plik GeoPackage otwierany
  biblioteką (warstwa `trees`, POINT, pola, wartości), round-trip CSV/XLSX/GeoJSON → import.
- **GDAL 3.12.4** (pyogrio; ten sam silnik, którym QGIS czyta GeoJSON i GeoPackage) na plikach wyeksportowanych
  z działającej aplikacji na PostgreSQL: oba pliki się otwierają, warstwa `trees`, EPSG:4326, Point, 4 punkty
  (drzewo bez lokalizacji pominięte), współrzędne w poprawnej kolejności, polskie znaki, liczby jako liczby.
- **Symulowany round-trip przez GDAL:** odczyt GeoPackage → przesunięcie punktu i zmiana atrybutu → zapis
  sterownikiem GDAL GeoJSON (tak zapisuje QGIS; człon `crs` = CRS84) → „Sprawdź plik” i import w ArboMachine:
  4 nowe drzewa, zmiana położenia i atrybutu zachowana, oryginał nietknięty, ostrzeżenia o duplikatach numerów.
- E2E UI (headless Chrome): zakładka „Import i eksport”, informacja o drzewach bez lokalizacji, instrukcja QGIS,
  pobranie GeoPackage, import z błędami po polsku i poprawny import, ekrany 390/768/1024 px bez poziomego scrolla.

# Rzeczy NIEprzetestowane

- **Otwarcie plików w prawdziwym QGIS** (na tym komputerze nie ma QGIS) — do wykonania wg checklisty
  w `docs/integrations/qgis.md`. Nie twierdzimy, że „QGIS działa”, dopóki ktoś nie otworzy plików w QGIS.
- Zapis z QGIS (prawdziwy plik z QGIS, inne wersje GDAL, opcje eksportu QGIS).
- Duże projekty (tysiące drzew) w GeoPackage / GeoJSON.
- Pliki od klientów / geodetów w innych układach.

# Hipotezy

- GeoPackage będzie wygodniejszy niż GeoJSON dla arborysty pracującego w QGIS (jeden plik, typy pól).
- QGIS służy głównie do przygotowania mapy końcowej, a dane rzadko wracają do systemu.
- Projekty arborysty używają PL-2000 albo PL-1992 — QGIS przeliczy WGS 84 w locie, więc V1 wystarcza
  do podglądu, ale przekazanie danych dalej może wymagać eksportu w układzie państwowym.
- Kody symboli wystarczą do stylowania w QGIS; gotowy styl QML byłby wygodnym kolejnym krokiem.

# Pytania do arborysty

1. Co dokładnie trafia obecnie z aplikacji terenowej do QGIS?
2. W jakim formacie?
3. Co jest następnie ręcznie poprawiane w QGIS?
4. Jakie warstwy powstają?
5. Jakie atrybuty drzewa muszą być widoczne?
6. Jak wyglądają etykiety?
7. Jakie symbole są stosowane?
8. Jakie kolory?
9. Jakie CRS występują (EPSG:4326, PL-1992, PL-2000 — która strefa, inne lokalne)?
10. Czy potrzebny jest GeoPackage?
11. Czy nadal wymagany jest SHP?
12. Czy potrzebny jest DXF?
13. Czy potrzebny jest gotowy QGIS Project?
14. Czy dane wracają z QGIS do systemu?
15. Czy QGIS jest tylko etapem przygotowania mapy końcowej?
16. Jakie pliki otrzymuje arborysta od projektantów / geodetów?
17. Jakie pliki przekazuje dalej?
18. Czy konkretne urzędy wymagają własnych schematów atrybutów?
19. Przy powrocie danych z QGIS: czy drzewa mają być aktualizowane (po numerze? po ID?), czy dodawane jako nowe?

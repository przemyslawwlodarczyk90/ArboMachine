# Do doprecyzowania

Decyzje biznesowe i techniczne, których nie da się dziś wiarygodnie podjąć. Dla każdej zapisano, co przyjęto
tymczasowo w V1 i czy blokuje dalszy development. Szersza lista pytań discovery: `konspekty/do doprecyzowania/`.

---

## Lokalizacja drzewa — dane pomiaru

### Pytanie
Czy oprócz współrzędnych należy zapisywać dokładność, źródło pomiaru (mapa / GNSS / ręcznie), status RTK (Fix/Float),
wysokość i czas pomiaru?

### Dlaczego wymaga doprecyzowania
Nie znamy jeszcze faktycznego workflow GNSS/RTK arborysty ani sprzętu, którego używa.

### Co przyjęto tymczasowo w V1
Przechowujemy wyłącznie `latitude`/`longitude` (obiekt `TreeLocation`, w API zagnieżdżony `location`).
Nowe dane pomiaru dojdą w `TreeLocation` / `location`, a nie jako luźne pola drzewa.

### Czy blokuje dalszy development?
NIE. Model może zostać rozszerzony w kolejnych zmianach schematu.

---

## Lokalizacja drzewa — układ współrzędnych i precyzja

### Pytanie
W jakim układzie arborysta dostaje i oddaje współrzędne (WGS84, PL-2000, PL-1992)? Jaka precyzja jest potrzebna?

### Dlaczego wymaga doprecyzowania
Urzędy i QGIS często pracują w PL-2000 / PL-1992, a GNSS i mapy webowe w WGS84. Nie wiemy, jakie pliki wymieniane są
z urzędem.

### Co przyjęto tymczasowo w V1
WGS84, stopnie dziesiętne; kolumny `numeric(10,7)` (7 miejsc po przecinku ≈ 1 cm). Więcej miejsc po przecinku jest
zaokrąglane, a nie odrzucane (odbiorniki GNSS podają często 8–9 miejsc). Transformacje układów — poza zakresem.

### Czy blokuje dalszy development?
NIE. Konwersja do innych układów może dojść po stronie eksportu/importu.

---

## Lokalizacja drzewa — ręczne wpisywanie współrzędnych

### Pytanie
Czy ręczne wpisywanie współrzędnych jest w ogóle potrzebne arboryście, czy wystarczy mapa i GNSS?

### Dlaczego wymaga doprecyzowania
Formularz współrzędnych to rozwiązanie przejściowe przed mapą; nie wiemy, czy w terenie ktoś przepisuje punkty ręcznie.

### Co przyjęto tymczasowo w V1
Karta drzewa ma sekcję „Location” z ręcznym formularzem (latitude/longitude). Backend nie zakłada źródła współrzędnych —
mapa/GNSS użyją tego samego endpointu `PUT .../trees/{treeId}/location`.

### Czy blokuje dalszy development?
NIE.

---

## Lokalizacja drzewa — PostGIS

### Pytanie
Kiedy przejść na `geometry(Point, 4326)` (PostGIS / Hibernate Spatial)?

### Dlaczego wymaga doprecyzowania
Dziś potrzebny jest tylko punkt drzewa. PostGIS ma sens przy zapytaniach przestrzennych, obszarach projektu, koronach,
strefach ochronnych, imporcie GIS i indeksach przestrzennych — żadne z nich nie jest jeszcze zdefiniowane.

### Co przyjęto tymczasowo w V1
Dwie kolumny liczbowe w tabeli `tree`, bez PostGIS. Punkt jest zamknięty w obiekcie `TreeLocation`, więc późniejsza
migracja zmieni głównie to miejsce i mapowanie (dane da się przenieść: `ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)`).

### Czy blokuje dalszy development?
NIE.

---

## Biblioteka mapowa

### Pytanie
Leaflet, OpenLayers czy MapLibre? Jakie podkłady (Geoportal, WMS, ortofotomapa), praca offline, wydajność przy dużej
liczbie drzew?

### Dlaczego wymaga doprecyzowania
Wybór zależy od WMS/Geoportalu, układów współrzędnych (OpenLayers lepiej obsługuje PL-2000), trybu offline i liczby
markerów — tego jeszcze nie wiemy.

### Co przyjęto tymczasowo w V1
Brak biblioteki mapowej. Zakładka „Map” w projekcie jest placeholderem. Frontend ma osobne komponenty
`TreeLocationSection` / `TreeLocationForm`, obok których może dojść `TreeLocationMap`.

### Czy blokuje dalszy development?
TAK dla etapu mapy — przed nim trzeba wybrać bibliotekę i podkład. NIE dla pozostałych prac.

---

## Migracje bazy danych

### Pytanie
Czy wprowadzić narzędzie migracji (Flyway / Liquibase) zamiast `spring.jpa.hibernate.ddl-auto=update`?

### Dlaczego wymaga doprecyzowania
To decyzja architektoniczna trudna do odwrócenia (wymaga baseline istniejącego schematu) — zgodnie z zasadą projektu
wymaga osobnej zgody. Przy PostGIS i zmianach typów kolumn `ddl-auto=update` przestanie wystarczać.

### Co przyjęto tymczasowo w V1
Nowe kolumny `latitude`, `longitude` (`numeric(10,7)`, nullable) dodaje Hibernate przy starcie aplikacji, jak dotychczas
wszystkie kolumny. Istniejące drzewa dostają `null` = „brak lokalizacji”.

### Czy blokuje dalszy development?
NIE teraz. TAK przed pierwszą zmianą, której `ddl-auto=update` nie wykona (zmiana typu, PostGIS, przeniesienie danych).

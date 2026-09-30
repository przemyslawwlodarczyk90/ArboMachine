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
Karta drzewa ma sekcję „Lokalizacja”: wskazanie punktu na mapie („Ustaw na mapie” / „Zmień na mapie”) oraz ręczny
formularz („Wpisz współrzędne”). Oba zapisują ten sam `TreeLocation` tym samym endpointem
`PUT .../trees/{treeId}/location`; backend nie zna źródła współrzędnych. Formularza nie usuwamy, dopóki discovery
nie potwierdzi, że jest zbędny.

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

## Biblioteka mapowa — DECYZJA V1 (etapy 11–12)

### Decyzja
**OpenLayers** (`ol`) jako jedyna biblioteka mapowa — pod przyszły workflow GIS (WMS, Geoportal, PL-2000/PL-1992,
QGIS). Nie oznacza to decyzji, jak te funkcje zostaną zrealizowane; żadna z nich nie jest zaimplementowana.

### Co działa w V1
Mapa projektu (`/projects/:projectId/map`, podgląd markerów drzew z lokalizacją) i mapa lokalizacji w karcie drzewa
(wybór punktu → „Zapisz lokalizację” / „Anuluj”). Dane w WGS84; EPSG:3857 tylko do renderowania.

### Nadal otwarte
Wydajność przy dużej liczbie drzew (clustering), praca offline, WMS/Geoportal — patrz sekcje poniżej.

---

## Podkład mapowy

### Pytanie
Jakich podkładów potrzebuje arborysta (ortofotomapa, Geoportal/WMS GUGiK, mapy klienta, własne rastry)?

### Dlaczego wymaga doprecyzowania
Brak realnego projektu pokazującego, na czym arborysta pracuje w terenie.

### Co przyjęto tymczasowo w V1
Publiczne kafelki OpenStreetMap (`tile.openstreetmap.org`). Ich polityka użycia nie dopuszcza intensywnego ruchu
produkcyjnego — przed wdrożeniem trzeba wybrać dostawcę podkładu (własny serwer kafelków, komercyjny dostawca,
WMS GUGiK).

### Czy blokuje dalszy development?
NIE dla developmentu. TAK przed wdrożeniem produkcyjnym.

---

## Mapa — domyślny widok i marker

### Pytanie
Od jakiego widoku startuje mapa, gdy projekt nie ma zlokalizowanych drzew? Co ma przedstawiać marker drzewa
(numer, gatunek, stan, prace, ryzyko)? Jakie etykiety są potrzebne?

### Dlaczego wymaga doprecyzowania
Projekt nie ma adresu/obszaru; symbolika arborystyczna nie jest znana.

### Co przyjęto tymczasowo w V1
Widok startowy: cała Polska (stały, bez geolokalizacji przeglądarki). Brak ograniczenia współrzędnych „tylko Polska”.
Jeden neutralny styl markera (zielony punkt; tymczasowy punkt przy wyborze — pomarańczowy). Brak etykiet na mapie —
numer i gatunek pokazuje panel po kliknięciu markera.

### Czy blokuje dalszy development?
NIE.

---

## Migracje bazy danych

### Pytanie
Czy wprowadzić narzędzie migracji (Flyway / Liquibase) zamiast `spring.jpa.hibernate.ddl-auto=update`?

### Dlaczego wymaga doprecyzowania
To decyzja architektoniczna trudna do odwrócenia (wymaga baseline istniejącego schematu) — zgodnie z zasadą projektu
wymaga osobnej zgody. Przy PostGIS i zmianach typów kolumn `ddl-auto=update` przestanie wystarczać.

### Co przyjęto tymczasowo w V1
Nowe kolumny `latitude`, `longitude` (`numeric(10,7)`, nullable) dodaje Hibernate przy starcie aplikacji, jak dotychczas
wszystkie kolumny. Istniejące drzewa dostają `null` = „brak lokalizacji”. Etapy 11–12 (mapa) nie zmieniły modelu DB.

### Czy blokuje dalszy development?
NIE teraz. TAK przed pierwszą zmianą, której `ddl-auto=update` nie wykona (zmiana typu, PostGIS, przeniesienie danych).

---

## Język interfejsu poza panelem arborysty

### Pytanie
Czy ekrany logowania/rejestracji/resetu hasła oraz panele urzędnika i administratora też mają być po polsku
(i kiedy)? Czy potrzebna będzie wielojęzyczność (biblioteka i18n)?

### Dlaczego wymaga doprecyzowania
Zakres polonizacji w etapach 11–12 obejmował panel arborysty. Pozostałe ekrany i komunikaty kont
(`userFacingErrorMessages.ts`, część „konta/logowanie”) są nadal po angielsku.

### Co przyjęto tymczasowo w V1
Polski UI: panel arborysty (projekty, drzewa, lokalizacja, mapa), górny pasek panelu, nazwy ról, ogólne komunikaty
błędów HTTP. Bez biblioteki i18n — teksty wprost w komponentach. Kod, endpointy, enumy i pola API po angielsku.

### Czy blokuje dalszy development?
NIE.

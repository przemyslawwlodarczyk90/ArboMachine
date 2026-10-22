# Do doprecyzowania

**Zasada od etapu 17:** brak potwierdzenia przez arborystę nie zatrzymuje developmentu. Elementy łatwe do zmiany
implementujemy jako sensowne V1 (i oznaczamy tutaj jako V1), a spotkanie z arborystą będzie testem działającego
systemu. Zatrzymujemy się tylko przed decyzjami fundamentalnymi, kosztownymi w migracji albo wymagającymi konkretnego
sprzętu/protokołu (szczegółowy model VTA, algorytmy waloryzacji, synchronizacja offline, konkretny GNSS).

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

**Etap 31:** arborysta wskazał podkłady wektorowe / QGIS, a dla prostszych zleceń rastrowe (Geoportal, Google).
Dodano wybór podkładu (OSM, ortofotomapa i mapa topograficzna Geoportalu, nakładka działek KIEG) i warstwy
referencyjne z plików QGIS — szczegóły i otwarte pytania w sekcji „Etap 31”. Polityka OSM przed produkcją — nadal OPEN.

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

## Język interfejsu — ROZSTRZYGNIĘTE w etapie 16 (V1)

### Decyzja
Cały interfejs użytkownika jest po polsku: wszystkie ekrany (także logowanie, rejestracja, reset hasła, „Mój profil”,
panele urzędnika i administratora, strona 404), powitania paneli z backendu i maile (rejestracja, reset hasła).
Kod, endpointy, enumy, pola API i surowe komunikaty błędów API pozostają po angielsku — frontend tłumaczy komunikaty
w `userFacingErrorMessages.ts`. Bez biblioteki i18n.

### Nadal otwarte
Czy kiedykolwiek potrzebna będzie wielojęzyczność (np. dla wykonawców spoza Polski)? Jeśli tak — wtedy i18n.
---

## Zdjęcia drzewa (TreePhoto) — etap 13

### Pytania (OPEN)
1. Kategorie zdjęć (np. ogólne, pień, korona, uszkodzenie) — czy w ogóle potrzebne?
2. Zdjęcie główne drzewa — czy jest i jak je wybrać?
3. Kolejność zdjęć — czy ma znaczenie (np. w dokumentacji)?
4. Podpisy/opisy zdjęć — czy potrzebne, kto je pisze, czy trafiają do dokumentu?
5. EXIF (data, orientacja, GPS z fotografii) — czy odczytywać i zapisywać?
6. Zewnętrzny aparat — jak zdjęcia trafiają z aparatu do aplikacji?
7. Automatyczne przypisanie zdjęcia do Tree (jak w obecnej aplikacji terenowej arborysty) — na jakiej podstawie?
8. Offline — czy zdjęcia muszą być robione i kolejkowane bez internetu?
9. Storage produkcyjny — dysk serwera, S3/MinIO, kopie zapasowe, retencja?
10. Sposób użycia zdjęć w dokumentacji (rozmiar, kadrowanie, miniatury, adnotacje).

### Dlaczego wymaga doprecyzowania
Brak realnego workflow fotograficznego arborysty (aparat vs tablet, aplikacja terenowa, ZWCAD, dokument końcowy).

### Co przyjęto tymczasowo w V1
- Encja `TreePhoto` = same metadane (nazwa oryginalna, typ, rozmiar, `createdAt`), bez podpisu, kategorii, zdjęcia
  głównego, kolejności, EXIF i adnotacji. Własność tylko przez `TreePhoto → Tree → Project → owner`.
- Plik na dysku serwera (`app.tree-photos.storage-directory`, domyślnie `storage/tree-photos`) pod kluczem UUID —
  za małą abstrakcją `PhotoStorage` (do podmiany na storage obiektowy). Bez BLOB w bazie.
- Formaty: JPEG, PNG, WebP; typ sprawdzany z zawartości pliku (sygnatura), nie z rozszerzenia. HEIC nieobsługiwany
  (iPhone przy wyborze z galerii zwykle wysyła JPEG — do sprawdzenia na realnym sprzęcie).
- Limit **15 MB na zdjęcie** (`app.tree-photos.max-file-size`) — rozsądna wartość developerska, nie wymaganie.
- Brak miniatur po stronie serwera — galeria wczytuje pełne pliki (przy wielu dużych zdjęciach na słabym łączu
  w terenie będzie to wolne; miniatury/kompresja do decyzji).
- Kolejność zapisu: plik → metadane (przy błędzie bazy plik jest usuwany); usuwanie: rekord → plik (błąd usunięcia
  pliku zostawia osierocony plik w logu, nigdy rekord bez pliku). Brak zadania sprzątającego osierocone pliki.
- Wybór pliku przez `<input type="file" accept="image/*">` — na tablecie/telefonie przeglądarka może zaproponować
  aparat; bez natywnej integracji z aparatem i bez zewnętrznego aparatu.

### Czy blokuje dalszy development?
NIE dla V1. TAK przed wdrożeniem produkcyjnym (storage, kopie zapasowe) i przed modułem dokumentacji.

---

## Zalecenia i prace przy drzewie (TreeWork) — etap 14

### Pytania (OPEN)
1. Słownik typów prac — rzeczywista lista prac arborysty.
2. Kody prac (np. oznaczenia używane w tabelach i na mapach).
3. Czy słownik jest globalny, konfigurowalny per użytkownik, czy per projekt/typ opracowania?
4. Symbole mapowe prac.
5. Kolory prac na mapie.
6. Reguły legendy (automatyczna? ręczna?).
7. Priorytet pracy.
8. Terminy wykonania.
9. Status wykonania (planowana / wykonana / odebrana?).
10. Parametry konkretnej pracy (np. procent redukcji korony, wysokość cięcia).
11. Wiele prac na jednym Tree — które ma pierwszeństwo na mapie, czy drzewo może mieć kilka symboli?
12. Kolejność prac przy drzewie.
13. Sposób przedstawienia prac w dokumentacji (program prac konserwatorskich, gospodarka drzewostanem, tabele).

### Dlaczego wymaga doprecyzowania
Arborysta używa zaleceń i symboli prac (część dodaje dziś w ZWCAD), ale nie mamy realnej listy, kodów ani symboli.

### Co przyjęto tymczasowo w V1
- Encja `TreeWork`: `name` (wymagana, wolny tekst, max 255) + `description` (opcjonalny, max 5000) + daty.
  Tree może mieć wiele prac. **Świadomie bez enuma `WorkType`**, priorytetu, statusu, terminu, wykonawcy, ceny,
  symbolu i koloru — String V1 zamiast wymyślonej domeny. Późniejsza migracja: `name` → odwołanie do słownika
  `WorkType` → symbol mapowy → reprezentacja w raporcie.
- Mapa nie jest powiązana z pracami (markery bez zmian), generator dokumentów nie istnieje.

- **Aktualizacja (etap 28):** pytania 7–9 mają V1 — status (Zalecenie / Zaplanowana / Wykonana / Anulowana),
  priorytet organizacyjny, termin i data wykonania; patrz sekcja „Gospodarka drzewostanem — etap 28”. Pytania 7–9
  pozostają OPEN jako weryfikacja hipotez V1.

### Czy blokuje dalszy development?
NIE dla V1. TAK przed symbolami prac na mapie, legendą i generowaniem dokumentacji.

---

## Przegląd Milestone 1 (etap 16) — braki zauważone, świadomie NIEzaimplementowane

Zauważone podczas przejścia pełnego flow; zapisane zamiast implementowane (etap 16 nie dodaje funkcji domenowych).

| Temat | Stan V1 | Do ustalenia |
|---|---|---|
| Usuwanie drzewa i projektu | Brak (nie ma DELETE Tree/Project) | Czy usuwać fizycznie, archiwizować, co ze zdjęciami (pliki) i pracami? |
| „Następne drzewo” / „Zapisz i dodaj następne” | Od etapu 17: „Dodaj następne drzewo” na karcie drzewa (numer → nowa karta) | Czy potrzebne „Poprzednie/Następne drzewo” i „Zapisz i dodaj następne”? Zależy od numeracji i kolejności pracy |
| Miniatury zdjęć | Galeria wczytuje pełne pliki | Generowanie miniatur/kompresja przy słabym łączu w terenie |
| Podsumowanie projektu | 5 liczb (drzewa, z/bez lokalizacji, zdjęcia, prace) | Czy potrzebne np. „drzewa ze zdjęciami”, „drzewa bez prac”, lista braków do uzupełnienia? |
| Zakładka projektu „Zdjęcia i dokumentacja” | Usunięta (zdjęcia są w karcie drzewa; stary link → podsumowanie) | Czy potrzebny widok wszystkich zdjęć projektu? Moduł dokumentacji — osobny etap |
| Powrót po wylogowaniu | Kolejne logowanie w tej samej karcie wraca na ostatnią stronę panelu (np. `/projects`) — zawsze na dane zalogowanego | Czy po wylogowaniu zawsze startować z pulpitu? |
| Ochrona przed opuszczeniem formularza z niezapisanymi zmianami | Brak (tylko „Anuluj” nie zapisuje) | Wymaga ingerencji w routing — do decyzji |
| Rozmiar paczki frontendu | ~650 kB JS (głównie OpenLayers) | Ewentualny podział na części (lazy loading mapy) przy wolnym internecie w terenie |

---

## Formularz terenowy drzewa (etap 17)

### Co przyjęto w V1
- Karta drzewa ma zawsze edytowalny formularz „Dane drzewa” w sekcjach: **Identyfikacja** (numer) → **Gatunek**
  (nazwa polska, łacińska) → **Pomiary** (obwód pnia [cm], wysokość drzewa [m], średnica korony [m]) → **Stan drzewa**
  → **Zagrożenia** → **Notatki**. Kolejność to UKŁAD V1, nie potwierdzony workflow arborysty.
- **Stan drzewa** (`conditionNotes`) i **Zagrożenia** (`hazardNotes`) to nowe pola V1 — wolny tekst (nullable `text`),
  bez skal, enumów, VTA i poziomów ryzyka. Łatwe do zastąpienia modelem strukturalnym po discovery.
- Dodawanie drzewa: sam numer → od razu karta terenowa. Na karcie „Dodaj następne drzewo” (zablokowane przy
  niezapisanych zmianach). Brak autonumeracji i nawigacji poprzednie/następne.
- Zapis tylko przyciskiem „Zapisz zmiany” (bez autozapisu); „Anuluj” przywraca zapisane wartości; komunikat
  „Masz niezapisane zmiany”. Brak blokady opuszczenia strony z niezapisanymi zmianami (wymaga ingerencji w routing).
- Pomiary: klawiatura numeryczna (`inputMode`), polski przecinek dziesiętny (12,5) — do API trafia liczba, wartości
  z API wyświetlane z przecinkiem. Limity zgodne z backendem (obwód 1–5000 cm, wysokość ≤ 150 m, korona ≤ 200 m).

### Nadal OPEN
1. Rzeczywista kolejność pracy przy jednym drzewie.
2. Które pola są obowiązkowe.
3. Które pola zależą od rodzaju projektu/zlecenia.
4. Numeracja (ręczna/automatyczna, format, unikalność, import numeracji klienta).
5. Gatunki (słownik, PL ↔ LAT, autocomplete, rodzaj, kultywary).
6. Wielopniowość (jak mierzony i zapisywany jest każdy pień).
7. Rzeczywiste pomiary (pierśnica vs obwód, wysokość pomiaru, metoda, dokładność, dwie osie korony).
8. Stan zdrowotny (tekst czy skala/kategorie).
9. Statyka.
10. Uszkodzenia (lista, lokalizacja na drzewie).
11. Zagrożenia (kategorie, poziomy ryzyka, strefa zagrożenia).
12. VTA.
13. Nawigacja „Poprzednie drzewo” / „Następne drzewo”.
14. „Zapisz i dodaj następne” w jednym kroku.

---

## Dyktowanie / speech-to-text (etap 18)

### Co przyjęto w V1
- Web Speech API przeglądarki (`SpeechRecognition` / `webkitSpeechRecognition`), język `pl-PL`, tylko wyniki końcowe.
  Izolacja: `speech/browserSpeechRecognition.ts` (jedyne miejsce znające API przeglądarki), `DictationController`
  (logika bez Reacta), `useSpeechDictation`, `VoiceInputButton`, `VoiceEnabledTextAreaField`.
- Przycisk „Dyktuj” przy polach opisowych: Opis stanu, Opis zagrożeń, Notatki drzewa oraz Opis pracy
  (Zalecenia i prace). Bez dyktowania: numer drzewa, nazwa łacińska, pomiary, współrzędne.
- Podyktowany tekst jest **dopisywany** na końcu pola (nigdy nie kasuje wpisanego tekstu), można go poprawić;
  nic nie zapisuje się automatycznie — dopiero „Zapisz zmiany”.
- Mikrofon startuje tylko po kliknięciu „Dyktuj”; widoczny stan „Słucham…”; jedna sesja naraz; wyłączenie
  przy opuszczeniu strony. Brak wsparcia przeglądarki (np. Firefox) → przycisk ukryty, formularz działa normalnie.
- Audio nie jest nagrywane ani wysyłane do backendu; brak AI/LLM i automatycznego wyciągania pomiarów z mowy.
  Uwaga: Chrome realizuje rozpoznawanie mowy po stronie usługi Google (wymaga internetu) — tekst mowy opuszcza
  urządzenie przez mechanizm przeglądarki, nie przez ArboMachine.

### Nadal OPEN
1. Które pola arborysta rzeczywiście dyktuje.
2. Dyktowanie nazw łacińskich (skuteczność polskiego rozpoznawania dla nazw botanicznych).
3. Dyktowanie danych strukturalnych (np. „obwód 120, wysokość 18” → pola).
4. Działanie dyktowania offline.
5. Kompatybilność docelowych tabletów i przeglądarek (Safari/iPadOS, przeglądarki Android).
6. Czy potrzebny jest fallback w postaci chmurowego speech-to-text.
7. Czy potrzebne są nagrania audio jako załączniki (obecna aplikacja terenowa arborysty pracuje z nagraniami).
8. Czy dyktowanie w opisie prac (TreeWork) jest potrzebne (V1: jest).
9. Interpunkcja i komendy głosowe („kropka”, „nowa linia”).

---

## Etap 31 — geolokalizacja, podkłady mapowe, oznaczenia drzew (po pierwszej odpowiedzi arborysty)

**Źródło:** pierwsza odpowiedź arborysty — pierwszy moduł to lokalizacja geodezyjna drzewa na podkładach wektorowych
(najlepiej QGIS) albo rastrowych (Geoportal, Google), punkt z odbiornika GNSS powiązany z kartą drzewa, wiele symboli
graficznych na jednym drzewie (wartościowe, do usunięcia, złom…), w przyszłości inne obiekty (ptaki, grzyby, znaleziska).
Pytania do arborysty: `konspekty/do doprecyzowania/!!!-dodatkowe-pytania-geolokalizacja-mapa-symbole.md`.

**Zasada etapu:** robimy to, co umiemy; w miejscu braku w UI jest widoczny znacznik „Do doprecyzowania” z krótkim
komentarzem (komponent `ToBeClarifiedNote`), a brak jest zapisany tutaj.

### Co zrobiono (V1)

| Obszar | V1 |
|---|---|
| Podkłady mapy | Wybór pod każdą mapą: OpenStreetMap, ortofotomapa Geoportalu (WMS `PZGIK/ORTO/WMS/HighResolution`), mapa topograficzna Geoportalu (WMS `TOPO`); nakładka działek ewidencyjnych KIEG (od zoomu 15, jeden obraz na widok). Wybór wspólny dla wszystkich map, zapamiętany na urządzeniu. Usługi sprawdzone 2026-10-20 w EPSG:3857. |
| PL-2000 / PL-1992 | Karta drzewa pokazuje punkt w PL-2000 (strefa z długości) i PL-1992 (X = północ, Y = wschód). Ręczne wpisanie współrzędnych także w PL-2000 strefy 5–8 i PL-1992. Raport PDF: wiersz „Układ krajowy”. Przeliczenia zgodne z PROJ do 1 mm (testy). |
| GNSS / RTK | Pomiar „Uśrednij 10 odczytów” (maks. 20 s) z rozrzutem; dokładność nie lepsza niż rozrzut. Progi: do 10 cm geodezyjna (RTK), do 1 m submetrowa. Odbiornik RTK przez „pozorowaną lokalizację” Androida (aplikacja odbiornika) — instrukcja w karcie drzewa. |
| Oznaczenia drzewa | Nowy moduł `treemarking`: katalog 16 oznaczeń w 5 grupach (wartość, postępowanie, stan, zagrożenie, siedlisko), wiele na drzewie; sekcja w karcie drzewa (działa offline), tryb mapy „Oznaczenia drzew” (rząd skrótów nad punktem, do 4 + „+N”, filtr, legenda), plakietki i filtr na liście drzew, eksport CSV/XLSX/GIS (`markings`), raport PDF. |
| Punkt ↔ karta | Karta drzewa: „Pokaż na mapie projektu” (mapa przybliża i zaznacza drzewo). Mapa: „Dodaj drzewo na mapie” — klik w miejsce → numer → karta drzewa z punktem (źródło MAP). |
| Podkład z QGIS | Nowy moduł `referencelayer`: plik GeoJSON / Shapefile ZIP / GeoPackage / DXF wgrany do projektu jako warstwa referencyjna (GeoJSON WGS 84 w bazie, tylko podgląd pod drzewami, kolor na warstwę, przełącznik widoczności). Czytniki GIS obsługują teraz linie (LineString / MultiLineString, DXF `LINE`). |
| Inne obiekty przyrodnicze | Tylko zaślepka na mapie (nieaktywny przycisk + „Do doprecyzowania”); do tego czasu oznaczenia z grupy „Siedlisko i przyroda”. |

### Otwarte (OPEN) i założenia bez potwierdzenia

| Temat | Stan |
|---|---|
| Model odbiornika GNSS | OPEN / BLOCKS STAGE (pełny RTK): producent, model, połączenie (Bluetooth / USB), aplikacja odbiornika, korekty (ASG-EUPOS / NTRIP), telefon czy tablet, Android czy iOS. Od tego zależy, czy wystarczy pozorowana lokalizacja, czy potrzebna aplikacja natywna. |
| Dane pomiaru RTK | OPEN: typ rozwiązania (FIX / FLOAT), PDOP, liczba satelitów, czas pomiaru, wysokość anteny — przeglądarka tych danych nie podaje; zapis wymagałby aplikacji natywnej albo odczytu NMEA. |
| Pomiar z odsunięciem (offset) | OPEN: pomiar obok pnia (pod koroną brak sygnału) i przesunięcie punktu o odległość/azymut — nie zrobione. |
| Uśrednianie | Założenie V1: 10 odczytów / 20 s, dokładność = max(średnia zgłoszonych dokładności, rozrzut). Do potwierdzenia, ile trwa pomiar przy drzewie. |
| Progi dokładności | Założenie V1: 10 cm / 1 m / 5 m / 15 m. Czy w dokumentacji wymagana jest konkretna dokładność? Czy blokować zapis słabego pomiaru? |
| Google Maps | OPEN: wymaga klucza Map Tiles API i akceptacji licencji (płatne powyżej limitu) — w UI opcja nieaktywna ze znacznikiem. Czy Geoportal + OSM wystarczą? |
| Inne podkłady GUGiK | OPEN: mapa zasadnicza (KIUT / GESUT), BDOT10k (usługa wymagała logowania przy próbie 2026-10-20), MPZP — które warstwy są potrzebne w terenie? |
| Podkłady offline | BRAK: kafle Geoportalu i OSM nie są zapisywane offline (service worker obsługuje tylko własny origin; polityka OSM zabrania masowego pobierania). |
| Warstwy referencyjne | Założenia V1: tylko podgląd (bez edycji), bez stylu z QGIS (kolor na warstwę), bez opisów / etykiet z atrybutów, bez kliknięcia obiektu, tylko na mapie projektu (nie na mapie lokalizacji drzewa ani w edytorze grup), limity 15 MB / 20 000 obiektów / 400 000 wierzchołków / 20 warstw, teksty z DXF pomijane, wymagają internetu. OPEN: przykładowy projekt QGIS arborysty. |
| Usługa WMS/WFS z QGIS Server | DEFERRED: zamiast plików — podłączenie serwera mapowego arborysty / gminy (adres usługi w projekcie). |
| Lista oznaczeń | Założenie V1: 16 oznaczeń, skróty i kolory wymyślone (nie branżowe). OPEN: lista i symbolika arborysty, edycja katalogu przez użytkownika, oznaczenia wyliczane z danych (waloryzacja → „wartościowe”, praca „wycinka” → „do usunięcia”). |
| Oznaczenia w imporcie | BRAK: kolumna `markings` jest tylko w eksporcie; import oznaczeń z plików — do doprecyzowania (nazwy czy kody?). |
| Oznaczenia na mapie | Założenie V1: widoczne od zoomu ok. 15, maks. 4 przy drzewie, tylko w trybie „Oznaczenia drzew” (nie mieszane z symbolami prac). Czy łączyć z symbolem drzewa / pracami? Czy symbole mają trafić do eksportu DXF / PDF z mapą? |
| Punkt ↔ karta | OPEN: kolejność w terenie (najpierw punkt czy karta), czy przesuwanie punktu na mapie jest dopuszczalne po pomiarze GNSS. V1: oba warianty dostępne. |
| Inne obiekty przyrodnicze | FUTURE / OPEN: typy, dane, powiązanie z drzewem, wymogi prawne (ochrona gatunkowa). |
| Weryfikacja | DO SPRAWDZENIA: wszystko sprawdzone w headless Chrome (desktop + telefon 390 px) na H2 i na PostgreSQL; NIE sprawdzone z prawdziwym odbiornikiem RTK, na tablecie w terenie ani z prawdziwym projektem QGIS. |

---

## Rejestr braków — stan po etapie 30

> Braki etapu 31 (geolokalizacja, podkłady, oznaczenia, warstwy z QGIS) są w sekcji „Etap 31” powyżej.

Jedno miejsce na **wszystko, czego brakuje**: funkcje, niesprawdzone obszary, dług techniczny i sprawy operacyjne.
Aktualizowany po każdym etapie (dopisujemy nowe braki, rozwiązane oznaczamy). Statusy: **OPEN** — decyzja/discovery,
**BRAK** — znana luka funkcjonalna, **DŁUG** — dług techniczny, **DO SPRAWDZENIA** — działa, ale nie zweryfikowane
w realnych warunkach, **OPERACYJNE** — sprawy środowiska.

### Funkcje produktu

| Obszar | Czego brakuje | Status |
|---|---|---|
| Drzewa | Usuwanie drzewa (co ze zdjęciami-plikami i pracami) | BRAK |
| Projekty | Usuwanie / archiwizacja projektu; konto z projektami nie może być usunięte (409 — polityka tymczasowa) | BRAK / OPEN |
| Drzewa | Filtry listy drzew — od etapów 27–29: waloryzacja, stan z ostatniej kontroli, grupa drzew, iglaste/liściaste; brak wyszukiwania, sortowania i filtrów „bez lokalizacji”, „bez zdjęć” | BRAK |
| Listy | Paginacja list projektów, drzew, zdjęć i prac (dziś zawsze całe listy) | DŁUG |
| Drzewa | Nawigacja „Poprzednie / Następne drzewo”, „Zapisz i dodaj następne” w jednym kroku (jest tylko „Dodaj następne drzewo”) | OPEN |
| Drzewa | Autonumeracja i unikalność numeru drzewa w projekcie | OPEN |
| Drzewa | Słownik gatunków, autocomplete, PL ↔ LAT | BRAK (V1 = wolny tekst) |
| Drzewa | Model wielopniowości, historia pomiarów, strukturalna ocena stanu / VTA / ryzyko | OPEN |
| Waloryzacja | V1 od etapu 27: zapis oceny arborysty (kategoria V1 + opisy); brak metody, skal, punktów, wycen PLN, historii ocen, waloryzacji grup drzew (grupy istnieją od etapu 29, ale bez oceny) | OPEN |
| Formularz | Blokada opuszczenia strony z niezapisanymi zmianami (jest tylko komunikat „Masz niezapisane zmiany”) | BRAK |
| Zdjęcia | Miniatury po stronie serwera / kompresja (galeria ładuje pełne pliki) | BRAK |
| Zdjęcia | Podpisy, kategorie, zdjęcie główne, kolejność, EXIF, HEIC | OPEN |
| Zdjęcia | Adnotacje (szkice cięć) — V1 od etapu 20 (linia, strzałka, odręcznie, tekst); właściwy zestaw narzędzi arborysty, edycja/przesuwanie elementów, gumka, powiększanie zdjęcia w edytorze | OPEN / BRAK |
| Zdjęcia | Zewnętrzny aparat i automatyczne przypisanie zdjęcia do drzewa | OPEN |
| Zdjęcia | Widok wszystkich zdjęć projektu — V1 od etapu 20 (zakładka „Dokumentacja”: zdjęcia pogrupowane po drzewie); brak filtrów, podglądu w pełnym rozmiarze i wyboru zdjęć do raportu | OPEN |
| Prace | Słownik typów prac, kody, parametry pracy | OPEN |
| Gospodarka | V1 od etapu 28: status, priorytet organizacyjny, termin, data wykonania, widok projektu z filtrami, tryb mapy; brak prac dla grup drzew i prac niezwiązanych z drzewem, prac cyklicznych, kosztów, wykonawców, protokołów, harmonogramu, historii zmian statusu, importu prac, warstwy prac w GeoPackage | OPEN / BRAK |
| Mapa | Symbole prac i drzew, legenda — V1 od etapu 19 (katalog techniczny); właściwe symbole arborysty, etykiety numerów drzew przy markerach | OPEN |
| Mapa | Klastrowanie przy dużej liczbie drzew; korony i strefy ochronne (obszary grup drzew — V1 od etapu 29; promienie pnia i korony z rysunku DXF nie są importowane) | BRAK / OPEN |
| Mapa | WMS / Geoportal / ortofotomapa; PostGIS. Układy PL-2000 / PL-1992 — od etapu 29 tylko przy imporcie (przeliczenie do WGS 84); mapa, eksport i współrzędne w karcie drzewa tylko w WGS 84 | OPEN |
| Mapa | Start mapy z obszaru/adresu projektu (dziś: widok Polski) | OPEN |
| Podsumowanie | Dodatkowe liczby (drzewa ze zdjęciami, bez prac, lista braków do uzupełnienia) | OPEN |
| Dokumentacja | „Dokument projektu” PDF — V1 od etapu 22 (jeden neutralny szablon; od etapu 29 dane zlecenia: zleceniodawca, adres obiektu, data zlecenia, monitoring i grupy drzew; autor nadal placeholderem); brak DOCX, mapy w dokumencie, szablonów per rodzaj opracowania, historii dokumentów, edycji przed eksportem | OPEN / BRAK |
| Import/eksport | CSV, XLSX, GeoJSON, GeoPackage, Shapefile ZIP, DXF (etapy 23–24, 29); arkusze aplikacji terenowych; obszary grup drzew; brak DWG, QGZ, QML, importu pliku projektu aplikacji terenowej (format własny), aktualizacji drzew przy imporcie, importu prac, kontroli i zdjęć, ręcznego wyboru układu współrzędnych, eksportu w PL-2000 | OPEN / BRAK |
| Teren | GPS urządzenia (etap 25) i praca offline z synchronizacją (etap 26) — V1; kontrole stanu offline od etapu 29; brak RTK/NTRIP, odbiorników Bluetooth, zdjęć i grup drzew offline, pobierania całego projektu „na wyjazd”, pełnego PWA | OPEN / BRAK |
| Dyktowanie | Dyktowanie danych strukturalnych, nazw łacińskich, komendy głosowe, offline, nagrania audio | OPEN |
| Monitoring | V1 od etapu 29: kontrola = data + ocena Dobry / Umiarkowany / Zły (umowna, nie VTA) + uwagi; brak strukturalnej karty oceny (pień, konary, korzenie, ubytki, posusz…), kontroli cyklicznych z terminem następnej, zdjęć przy kontroli, oceny ryzyka | OPEN |
| Powiadomienia | V1 od etapu 30: lista w aplikacji + Web Push (ocena „Zły”, prace na jutro, test); brak e-mail/SMS, ustawień, które powiadomienia otrzymywać i o której, oznaczania jako przeczytane, powiadomień dla urzędu/zleceniodawcy | OPEN / BRAK |
| Kalendarz | V1 od etapu 30: jednorazowy eksport `.ics` prac z terminem; brak subskrypcji kalendarza (adres URL odświeżany automatycznie), synchronizacji dwukierunkowej, godzin prac | OPEN / BRAK |
| Grupy drzew | V1 od etapu 29: obszar (wielokąt) + nazwa + uwagi + przypisane drzewa; brak typów (szpaler, aleja, zadrzewienie, porost), pomiarów grupy, waloryzacji i prac dla grupy, edycji wierzchołków istniejącego obszaru (jest tylko narysowanie od nowa), pracy offline | OPEN / BRAK |
| Konto | Zmiana hasła z poziomu „Mój profil” (dziś tylko przez „Nie pamiętasz hasła?”) | BRAK |
| Konto | Po „Wyloguj” kolejne logowanie w tej samej karcie wraca na ostatnią stronę panelu (np. `/projects`) | OPEN |
| Role | Panel urzędnika — tylko pulpit z zapowiedziami modułów; brak workflow urząd ↔ wykonawca, organizacji | BRAK / FUTURE |
| Panel arborysty | Etap 32: **usunięto zakładki modułów planowanych** (Oceny drzew, Raporty z prac, Wnioski, Harmonogram), poza zamówieniem klienta; stare adresy `/arborist/...` przekierowują na „Projekty”. Backend `/api/arborist/panel` nie jest już wywoływany przez frontend — kandydat do usunięcia (decyzja w „KONSPEKTY NOWE/03-DECYZJE-V1.md”) | ROZWIĄZANE / OPEN |
| Współpraca | Wielu użytkowników w jednym projekcie (ProjectMembership) | FUTURE |
| Język | Wielojęzyczność (i18n) — dziś tylko polski UI | OPEN |

### Niesprawdzone w realnych warunkach

| Obszar | Czego nie sprawdzono | Status |
|---|---|---|
| Dyktowanie | Prawdziwy głos i prawdziwy mikrofon — testy E2E używały podstawionego SpeechRecognition | DO SPRAWDZENIA |
| Dyktowanie | Docelowe tablety/przeglądarki (Safari na iPadzie, Chrome na Androidzie), jakość polskiego rozpoznawania w terenie | DO SPRAWDZENIA |
| Dyktowanie | Prywatność: Chrome rozpoznaje mowę przez usługę Google (wymaga internetu, dźwięk opuszcza urządzenie przez przeglądarkę) | OPEN |
| Tablet | Obsługa dotykiem na fizycznym tablecie (testy tylko w emulowanych rozmiarach ekranu) | DO SPRAWDZENIA |
| GPS | Dokładność i czas pomiaru GPS tabletu w terenie (pod koronami, przy budynkach) — testy tylko z emulowaną pozycją | DO SPRAWDZENIA |
| Offline | Praca bez zasięgu na prawdziwym tablecie (przełączanie sieci, uśpienie, Safari/iPad, setki zmian) — testy w emulowanym braku sieci | DO SPRAWDZENIA |
| Adnotacje | Rysowanie palcem i rysikiem na FIZYCZNYM tablecie, eksport PNG z dużych zdjęć (12+ Mpx) na słabszym tablecie (pamięć, czas). Sprawdzone w etapie 22 E2E w headless Chrome: mysz, emulowany dotyk, tekst, zapis, anulowanie, ekrany 390/768/1024 px | DO SPRAWDZENIA |
| Dokumentacja | Wydajność zakładki „Dokumentacja” przy setkach zdjęć (każda miniatura pobiera pełny plik) | DO SPRAWDZENIA |
| Dokument PDF | Czas i pamięć generowania przy dużym projekcie (setki drzew i zdjęć) — generowanie synchroniczne, cały PDF w pamięci; jakość wydruku zdjęć (1600 px) i czytelność na papierze | DO SPRAWDZENIA |
| Zdjęcia | Aparat tabletu przez `<input type="file" accept="image/*">`, duże zdjęcia (8–15 MB) na słabym łączu | DO SPRAWDZENIA |
| Mapa | Wydajność przy setkach/tysiącach drzew | DO SPRAWDZENIA |
| QGIS | Otwarcie eksportów GeoJSON / GeoPackage w PRAWDZIWYM QGIS i import pliku zapisanego przez QGIS — sprawdzone tylko GDAL 3.12.4 (checklista: `docs/integrations/qgis.md`) | DO SPRAWDZENIA |
| Mapa | Czytelność symboli i znaczków prac na gęstym podkładzie OSM, na ortofotomapie i w wydruku | DO SPRAWDZENIA |
| Web Push | Powiadomienia na prawdziwym telefonie/tablecie: Android Chrome, iPad/iPhone (Safari wymaga aplikacji dodanej do ekranu głównego, iOS 16.4+). Sprawdzone: headless Chrome → prawdziwy FCM przyjął wiadomość | DO SPRAWDZENIA |
| Kalendarz | Import pliku `.ics` w Google Calendar, Outlook i kalendarzu Apple (testy: zgodność z RFC 5545 w testach jednostkowych) | DO SPRAWDZENIA |
| GIS | Pliki DXF z innych programów niż aplikacja terenowa arborysty (inne nazwy warstw, bloki, MTEXT, DWG zapisany jako DXF); Shapefile i GeoPackage z QGIS w PL-2000 — sprawdzone na plikach własnych i GDAL 3.12.4 | DO SPRAWDZENIA |
| Dokument PDF | Projekt 938 drzew: 530 stron, 1,4 MB, 4,4 s — czy taki dokument jest użyteczny (czy karty wszystkich drzew mają być w PDF)? | DO SPRAWDZENIA |
| Maile | Wysyłka maili rejestracji/resetu przez prawdziwy SMTP (lokalnie potwierdzanie maila jest wyłączalne) | DO SPRAWDZENIA |

### Dług techniczny

| Obszar | Opis | Status |
|---|---|---|
| Baza | Brak Flyway/Liquibase — schemat aktualizuje `ddl-auto=update` (nowe kolumny tekstowe z etapu 17 dodał Hibernate) | DŁUG |
| Testy backendu | Testy działają na H2, nie na PostgreSQL (błąd tylko-PostgreSQL wyszedł już w etapie 2); rozważyć Testcontainers | DŁUG |
| Testy frontendu | Tylko testy logiki (`node --test`); brak testów renderowania komponentów React (od etapu 28 także widok „Gospodarka drzewostanem” i formularz statusu/priorytetu testowane tylko przez logikę + E2E) | DŁUG |
| Gospodarka | Filtr listy prac projektu w backendzie działa w pamięci (całe prace projektu z bazy), frontend i tak filtruje lokalnie; przy tysiącach prac — zapytanie z WHERE / paginacja | DŁUG |
| Gospodarka | `TreeWorkExportData.isOpen()` i `ProjectDocumentWorkData.isOpen()` porównują kody statusów jako tekst (duplikat `TreeWorkStatus.isOpen()` — moduły eksportu/dokumentu nie zależą od encji) | DŁUG |
| Testy E2E | Scenariusze E2E (headless Chrome przez CDP) istnieją tylko lokalnie poza repozytorium — brak E2E w repo i CI | DŁUG |
| Jakość | Brak lintera (ESLint) i CI uruchamiającego testy przy commitach | DŁUG |
| Frontend | Paczka JS ~700 kB (głównie OpenLayers) — brak podziału na części (lazy loading mapy i edytora adnotacji) | DŁUG |
| Build | Jar ma ~100 MB: `target/classes/static` zachowuje stare paczki frontendu z poprzednich buildów (czyszczone tylko `src/main/resources/static`); GeoPackage dokłada ~16 MB natywnego SQLite | DŁUG |
| Bezpieczeństwo | Token JWT w `localStorage`, brak odświeżania tokenu (sesja wygasa po czasie ważności) | OPEN |
| Zdjęcia | Brak zadania sprzątającego osierocone pliki (plik bez rekordu po nieudanym usunięciu; od etapu 20 także stare PNG adnotacji) | DŁUG |
| Adnotacje | PNG adnotacji sprawdzany tylko po sygnaturze i rozmiarze — backend nie dekoduje obrazu ani nie sprawdza wymiarów | DŁUG |
| Dokument PDF | Zdjęcia bez obrotu wg EXIF; WebP bez adnotacji nie trafia do PDF; brak miniatur — generator za każdym razem dekoduje i zmniejsza pełne zdjęcia | DŁUG |
| Zdjęcia | Storage produkcyjny i kopie zapasowe plików (dziś katalog na dysku serwera) | OPEN |
| Mapa | Publiczne kafelki OpenStreetMap — polityka użycia nie dopuszcza ruchu produkcyjnego | OPEN |
| Powiadomienia | Kolejka wysyłki Web Push w pamięci procesu (pula wątków) — restart w trakcie wysyłki gubi wiadomości (powiadomienie w liście zostaje) | DŁUG |
| Powiadomienia | Przypomnienia uruchamia `@Scheduled` w każdej instancji — przy kilku instancjach potrzebna blokada (np. ShedLock); deduplikacja w bazie chroni przed podwójnym zapisem, nie przed podwójnym pushem | DŁUG |
| Bezpieczeństwo | Wygenerowany klucz prywatny VAPID zapisany w bazie bez szyfrowania (produkcja: podać klucze w konfiguracji / sejfie sekretów) | DŁUG |
| GIS | Numery drzew z rysunku DXF dopasowywane do punktów heurystyką położenia etykiet (na pliku arborysty 836/839 poprawnie) — ostrzeżenie w imporcie, ale bez ręcznej korekty dopasowań | DŁUG |
| GIS | Brak sprawdzania samoprzecięć wielokątów grup drzew; pole liczone na sferze (przybliżenie, błąd < 0,5% dla małych obszarów) | DŁUG |
| GIS | Układy współrzędnych tylko PL-2000 (strefy 5–8) i PL-1992, rozpoznawane z `.prj` / SRS albo zgadywane z zakresu liczb; brak biblioteki PROJ, innych EPSG i transformacji datum | DŁUG |
| GIS | Klasyfikacja iglaste/liściaste po nazwie gatunku zdublowana w backendzie (`SpeciesGroupClassifier`) i frontendzie (`speciesGroup.ts`); miłorząb traktowany jako liściasty | DŁUG |
| Monitoring | Ostatnia kontrola każdego drzewa (`/monitoring/latest`) liczona w pamięci z wszystkich kontroli projektu | DŁUG |
| Testy E2E | Skrypt danych demonstracyjnych (`scripts/demo`) jest w repo, ale scenariusze E2E nadal poza repo | DŁUG |

### Sprawy operacyjne (środowisko lokalne)

| Opis | Status |
|---|---|
| W bazie deweloperskiej zostały konta i projekty testowe z adresami `...@example.com` (z testów E2E etapów 11–26) — do usunięcia przy okazji | OPERACYJNE |
| Katalog `storage/tree-photos` może zawierać pliki testowe z E2E (w `.gitignore`) | OPERACYJNE |
| Lokalne, niezacommitowane zmiany właściciela repo: `application.properties` (port 8083), `tsconfig.tsbuildinfo` | OPERACYJNE |
| Produkcja: HTTPS (wymagany przez Web Push i service worker), stały `app.push.vapid-subject` z prawdziwym adresem, kopie zapasowe bazy i katalogu zdjęć — patrz `docs/WYDANIE.md` | OPERACYJNE |

---

## Symbole mapowe — etap 19

**Status:** V1 DECISION / DISCOVERY REQUIRED

### Decyzje V1
- `Tree.mapSymbolCode` i `TreeWork.mapSymbolCode` — opcjonalny kod (nullable `varchar(64)`), nie enum w bazie.
  Istniejące rekordy mają `null` i wyglądają jak dotąd (neutralny punkt).
- Katalog symboli jest **w kodzie** (`MapSymbolCatalog`), nie w bazie; `GET /api/map-symbols`. Backend przyjmuje
  tylko kody z katalogu (bez CSS, SVG, ścieżek, URL). Symbole to techniczne style (kształt + kolor), a nie typy zabiegów.
- Renderer V1: OpenLayers `CircleStyle` / `RegularShape`; opis kształtu wspólny dla mapy i miniatur SVG w UI
  (`mapSymbolShapeSpec.ts`), więc później można przejść na `ol/style/Icon` + pliki SVG bez zmiany danych.
- Symbol drzewa = centralny marker; symbole prac = małe znaczki wokół drzewa (przesunięcie tylko wizualne — współrzędne
  TreeLocation bez zmian). Brak reguły „pierwsza/ostatnia praca wygrywa”.
- Legenda wyliczana z danych, niezapisywana. Tekst wpisu pracy = `TreeWork.name` (nie znaczenie koloru).
- Symbol zmienia się osobnymi operacjami `PUT/DELETE .../map-symbol` (drzewo i praca), niezależnie od formularza danych.
- Mapa pobiera prace całego projektu jednym zapytaniem `GET /api/projects/{projectId}/works` (bez N+1).

### Założenia przyjęte bez potwierdzenia (NIEWIADOME — do sprawdzenia z arborystą)
| Założenie V1 | Dlaczego niepewne |
|---|---|
| Katalog 6 symboli: Zielone koło, Niebieski kwadrat, Pomarańczowy trójkąt, Czerwony krzyż, Fioletowy romb, Czarny iks | Wymyślony zestaw techniczny — prawdziwa biblioteka symboli arborysty (aplikacja terenowa / ZWCAD) nieznana |
| Kolory: `#7CB342`, `#1565C0`, `#EF6C00`, `#C62828`, `#6A1B9A`, `#212121`, biała obwódka | Dobrane „na oko”; nie sprawdzone na ortofotomapie, w druku ani dla osób z zaburzeniami widzenia barw |
| Zielone koło rozjaśnione (`#7CB342`), bo pierwotny odcień był nie do odróżnienia od neutralnego markera (`#2f6b3c`) | Neutralny marker też jest zielony — czy drzewo bez symbolu ma w ogóle być zielone? |
| Rozmiary: symbol drzewa 8 px, znaczek pracy 6 px | Na gęstym podkładzie OSM znaczki są małe i konkurują z ikonami POI |
| Maks. 3 znaczki prac przy drzewie + „+N” (pozycje: góra-prawo, góra-lewo, dół-prawo; „+N” dół-lewo) | Nie wiadomo, ile symboli arborysta chce widzieć i który ma pierwszeństwo |
| Kolejność znaczków = kolejność dodania prac | Brak zasad priorytetu symboli |
| Zmiana symbolu przesuwa `updatedAt` drzewa/pracy | Symbol to prezentacja — czy ma liczyć się jako zmiana danych drzewa? |
| Przy cudzym drzewie najpierw 404 (własność), dopiero potem 400 (zły kod) | Świadoma decyzja bezpieczeństwa — do potwierdzenia jako reguła ogólna |
| Symbol pracy nie jest częścią formularza dodawania/edycji pracy (tylko osobny edytor „Zmień symbol”) | Może arborysta woli wybierać symbol od razu przy dodawaniu pracy |
| Legenda: zawsze „Drzewo bez symbolu” + symbole drzew jako „Symbol drzewa — nazwa symbolu”; prace deduplikowane po (nazwa bez rozróżniania wielkości liter, kod) | Nie wiadomo, jak ma wyglądać legenda końcowa w dokumentacji |
| Legenda i znaczki uwzględniają tylko drzewa z lokalizacją | Drzewa bez punktu nie mają na mapie żadnej reprezentacji |
| Panel wybranego drzewa pokazuje wszystkie jego prace (także bez symbolu) | — |
| Katalog symboli wymaga zalogowania | Nie jest poufny; decyzja porządkowa |
| Mapa i symbole mają stałe kolory także w ciemnym motywie | Nie sprawdzano czytelności w trybie ciemnym |

### Nadal OPEN
- Rzeczywiste znaczenie symboli używanych przez arborystę i biblioteka symboli z aplikacji terenowej / ZWCAD.
- Powiązanie rodzaju pracy (przyszły WorkType) → domyślny symbol, z możliwością nadpisania.
- Czy drzewo potrzebuje własnego symbolu, czy wystarczą symbole prac.
- Priorytet symboli, maksymalna liczba symboli przy drzewie, symbolika wielu zaleceń.
- Sposób legendy końcowej (mapa w dokumentacji, druk).
- Kolorystyka na ortofotomapie i na białym tle (wydruk), kontrast, daltonizm.
- Format eksportu stylu do QGIS (SVG Tiny 1.2, QML/SLD) i DXF.
- Własne symbole użytkownika (upload SVG — wymaga osobnego projektu bezpieczeństwa i sanityzacji).
- Etykiety numerów drzew przy markerach (dziś brak).

---

## Adnotacje fotograficzne — etap 20

**Status:** V1 DECISION / DISCOVERY REQUIRED

### Decyzje V1
- Oryginalne `TreePhoto` nigdy nie jest nadpisywane. Adnotacja to osobna encja `TreePhotoAnnotation`
  (tabela `tree_photo_annotation`, `tree_photo_id` UNIQUE) — jedna **aktualna** adnotacja na zdjęcie, bez historii wersji.
- Adnotacja = edytowalny dokument wektorowy (`document_json`, TEXT, `schemaVersion = 1`) + osobny spłaszczony PNG
  (zdjęcie + rysunek) w istniejącym `PhotoStorage`, pod kluczem UUID (klucz nie wychodzi do frontendu).
- Współrzędne znormalizowane 0..1 względem zdjęcia (nie piksele ekranu), zaokrąglane do 6 miejsc po przecinku.
- Narzędzia V1: `LINE`, `ARROW`, `FREEHAND`, `TEXT`. Paleta: `RED`, `YELLOW`, `BLUE`, `BLACK`, `WHITE`;
  grubości `SMALL` / `MEDIUM` / `LARGE`. Nazwy techniczne po angielsku, UI po polsku; kolor nie ma znaczenia arborystycznego.
- Renderer V1: natywny Canvas 2D (bez Fabric.js / Konva); ten sam kod rysuje podgląd i PNG do dokumentacji.
  Pointer Events — mysz, dotyk i rysik jednym kodem; bez obsługi nacisku rysika.
- Cofnij / Ponów / Wyczyść tylko w bieżącej sesji edycji (frontend); backend przechowuje wyłącznie aktualny dokument.
- Backend waliduje cały dokument przed zapisem i zapisuje dokument po walidacji, nigdy surowy JSON.
- Zapis: najpierw NOWY PNG, potem metadane; błąd bazy → usunięcie nowego pliku; stary PNG usuwany dopiero po udanym
  zapisie (best effort). Usunięcie zdjęcia usuwa także adnotację i jej PNG; usunięcie adnotacji nie rusza oryginału.
- `GET .../photos/{photoId}/documentation-content` = wersja z adnotacją, jeśli istnieje, w przeciwnym razie oryginał —
  stabilny kontrakt dla przyszłego generatora dokumentów.
- `TreePhotoResponse` rozszerzony o `hasAnnotation` i `annotationUpdatedAt`; nowe `GET /api/projects/{projectId}/photos`
  (zdjęcia całego projektu jednym zapytaniem, bez N+1).
- `/projects/:projectId/documentation` to zakładka „Dokumentacja” → „Materiały do dokumentacji” (przegląd zdjęć
  pogrupowanych po drzewie). To NIE jest generator PDF/DOCX.

### Założenia przyjęte bez potwierdzenia (NIEWIADOME — do sprawdzenia z arborystą)
| Założenie V1 | Dlaczego niepewne |
|---|---|
| Limity techniczne: maks. 200 elementów, 1000 punktów w linii odręcznej, 20 000 punktów łącznie, tekst do 500 znaków, id elementu do 64 znaków `[A-Za-z0-9_-]` | Ochrona przed ogromnymi dokumentami, dobrane bez danych z terenu |
| Kolory: czerwony `#E53935`, żółty `#FDD835`, niebieski `#1E88E5`, czarny, biały | Dobrane „na oko”; nie sprawdzone w druku ani na różnych zdjęciach |
| Grubości względne do krótszego boku zdjęcia: 0,4% / 0,8% / 1,4%; tekst 3,5% / 5% / 7% | Nie wiadomo, jakich grubości używa arborysta (ZWCAD) |
| Tekst: jedna linia, lewy górny róg w miejscu dotknięcia, pogrubiony, z kontrastową obwódką; bez zawijania (może wyjść poza zdjęcie) | Brak wiedzy o tym, jak opisuje się szkice |
| Tekst wstawia się przez: wpisanie tekstu w pole, potem dotknięcie zdjęcia | Wygodniejszy przepływ do ustalenia na tablecie |
| Grot strzałki tylko na końcu odcinka | Może potrzebne strzałki dwustronne |
| Linia/strzałka krótsza niż 0,5% zdjęcia (przypadkowe dotknięcie) nie jest zapisywana; linia odręczna dopisuje punkt co ≥ 0,2% | Progi dobrane bez testów na tablecie |
| Brak edycji/przesuwania istniejących elementów i gumki — tylko Cofnij / Wyczyść | Nie wiadomo, czy arborysta tego potrzebuje |
| Pustej adnotacji nie da się zapisać — trzeba użyć „Usuń adnotacje” (backend odrzuca dokument bez elementów) | Świadoma decyzja porządkowa |
| Brak adnotacji → `GET .../annotation` zwraca 404 (jak każdy nieistniejący zasób), nie 204 | Konwencja API |
| PNG do dokumentacji: dłuższy bok maks. 2560 px (mniejsze zdjęcia w oryginalnej rozdzielczości) | Wymagana rozdzielczość druku nieznana |
| Limit rozmiaru PNG = limit rozmiaru zdjęcia (`tree-photos.max-file-size`) | PNG dużego zdjęcia może przekroczyć limit — wtedy zapis się nie uda |
| Miniatura w galerii drzewa i podgląd pokazują wersję do dokumentacji (z adnotacją); oryginał widać w edytorze | Może arborysta chce przełączać oryginał/adnotację w podglądzie |
| Brak pliku PNG adnotacji na dysku → `documentation-content` oddaje oryginał (log błędu), `annotation/content` → 404 | Lepsze zdjęcie bez szkicu niż brak zdjęcia w dokumentacji |
| Treści adnotacji/dokumentacji wysyłane z `Cache-Control: no-store` | Zmieniają się po edycji |
| Pobrany plik adnotowany nazywa się `<nazwa zdjęcia>-adnotacja.png` | — |
| Edycja adnotacji nie zmienia `updatedAt` drzewa ani zdjęcia | Czy adnotacja ma być zmianą danych drzewa? |
| Zakładka „Dokumentacja” pokazuje wszystkie zdjęcia (bez wyboru, kolejności i podpisów do raportu), drzewa i zdjęcia w kolejności dodania | Układ raportu nieznany |

### Nadal OPEN
- Dokładny zestaw narzędzi używanych przez arborystę (dziś w ZWCAD); czy potrzebne prostokąt / wielokąt.
- Znaczenie kolorów adnotacji i preferowane grubości linii.
- Czy potrzebna trwała historia zmian (undo po ponownym otwarciu) i kilka wariantów adnotacji jednego zdjęcia.
- Czy adnotacja ma autora, datę, podpis.
- Rozdzielczość zdjęcia do dokumentacji, wymagania druku.
- Czy dokument zawsze używa wersji z adnotacją (dziś tak — `documentation-content`).
- Pozycjonowanie zdjęć w raporcie, podpisy pod zdjęciami, wybór zdjęć do raportu.
- Adnotacje zdjęć z zewnętrznego aparatu (import zdjęć spoza tabletu).

---

## Dokumentacja — etapy 21–22 („Dokument projektu”, PDF)

**Status:** V1 IMPLEMENTED / DISCOVERY REQUIRED. Szczegóły: `docs/discovery/etap-21-dokumentacja-discovery.md`.
Statusy w tej sekcji: **V1 IMPLEMENTED** — działa w V1; **OPEN DOMAIN QUESTION** — pytanie do arborysty;
**DEFERRED** — świadomie odłożone; **TECHNICAL DECISION** — decyzja techniczna V1, łatwa do zmiany.

| Temat | Stan |
|---|---|
| Rodzaje dokumentów | V1 IMPLEMENTED: jeden neutralny „Dokument projektu”. OPEN DOMAIN QUESTION: program prac konserwatorskich, inwentaryzacja, waloryzacja, gospodarka drzewostanem, ochrona drzew na budowie — czym się różnią? |
| Wymagane sekcje | V1 IMPLEMENTED: strona tytułowa, informacje o projekcie, podsumowanie, mapa (tekst), zestawienie drzew, karty drzew. OPEN DOMAIN QUESTION: które sekcje są obowiązkowe, które zależą od rodzaju opracowania, co arborysta pisze zawsze ręcznie? |
| Dane klienta / zleceniodawcy | V1 IMPLEMENTED: placeholder „Dane do uzupełnienia”. OPEN DOMAIN QUESTION: jakie dane, czy należą do projektu czy do dokumentu? Brak encji `Client`. |
| Dane autora | V1 IMPLEMENTED: placeholder. OPEN DOMAIN QUESTION: imię, nazwisko, firma, numer uprawnień — z profilu konta czy per dokument? |
| Kwalifikacje | OPEN DOMAIN QUESTION: jakie kwalifikacje / certyfikaty muszą być w dokumencie. Brak modelu. |
| Dane obiektu (adres, działka) | V1 IMPLEMENTED: placeholder. OPEN DOMAIN QUESTION: zakres danych obiektu. |
| Numeracja dokumentu | DEFERRED: brak numeru dokumentu. OPEN DOMAIN QUESTION: czy wymagany i w jakim formacie. |
| Wersjonowanie / historia | TECHNICAL DECISION: PDF generowany na żądanie, bez zapisu pliku i bez encji `ProjectDocument`. DEFERRED: historia wygenerowanych dokumentów. |
| Status roboczy / ostateczny | V1 IMPLEMENTED: dokument zawsze oznaczony jako roboczy (wersja MVP). OPEN DOMAIN QUESTION: czy potrzebny status i zatwierdzanie. |
| Podpisy / pieczęcie | DEFERRED. OPEN DOMAIN QUESTION: podpis odręczny na wydruku, pieczęć, podpis elektroniczny? |
| Logo / branding | DEFERRED: tylko napis „ArboMachine”. OPEN DOMAIN QUESTION: logo i stopka firmy arborysty. |
| Układ tabel | V1 IMPLEMENTED: jedna tabela drzew (nr, gatunek PL, gatunek LAT, obwód, wysokość, średnica korony), nagłówek powtarzany na kolejnych stronach. OPEN DOMAIN QUESTION: kolumny, kolejność, sortowanie, tabela zbiorcza prac, numeracja tabel. |
| Układ zdjęć | V1 IMPLEMENTED: zdjęcia pod kartą drzewa, wszystkie, „Fot. N. Drzewo nr X — nazwa pliku (z adnotacją)”. OPEN DOMAIN QUESTION: przy drzewach, w osobnym rozdziale czy w załączniku? Wybór zdjęć? Własne podpisy? Ile zdjęć? |
| Zdjęcia z adnotacją | V1 IMPLEMENTED: wersja z adnotacją, jeśli istnieje (jak `.../documentation-content`), w przeciwnym razie oryginał. |
| Mapy w dokumencie | DEFERRED / OPEN: zamiast obrazu mapy tekst „Mapa projektu dostępna jest w aplikacji ArboMachine.” i współrzędne w kartach drzew. Snapshot OpenLayers wymaga renderowania mapy po stronie serwera albo przesyłania obrazu z przeglądarki. OPEN DOMAIN QUESTION: mapa w dokumencie czy załącznik, skala, podkład, legenda. |
| Załączniki | DEFERRED. OPEN DOMAIN QUESTION: jakie załączniki (mapy, tabele, zdjęcia) i w jakiej formie. |
| PDF | V1 IMPLEMENTED: `GET /api/projects/{projectId}/documents/project-report` → `application/pdf`, plik `arbomachine-projekt-{id}.pdf`. |
| DOCX | DEFERRED. OPEN DOMAIN QUESTION: czy arborysta potrzebuje edytowalnego DOCX? |
| Ręczna edycja przed eksportem | DEFERRED (brak edytora). OPEN DOMAIN QUESTION: co trzeba zmieniać przed wysłaniem — DOCX, edytowalne pola, rich text czy wersja robocza dokumentu? |
| Różne szablony | TECHNICAL DECISION: interfejs `DocumentGenerator` + szablon `templates/documents/project-report.html`; nowy rodzaj = nowy generator i szablon. OPEN DOMAIN QUESTION: jakie szablony. |
| Szablony klientów / urzędów | OPEN DOMAIN QUESTION: czy klienci / urzędy mają własne wzory? |
| Stan drzewa i zagrożenia w dokumencie | V1 IMPLEMENTED: wolny tekst z formularza terenowego (tylko niepuste). OPEN DOMAIN QUESTION: czy tak ma wyglądać ocena w dokumencie. |

### Założenia techniczne V1 (TECHNICAL DECISION — bez potwierdzenia)
| Założenie | Uwagi |
|---|---|
| Pipeline: Thymeleaf → HTML → jsoup (DOM) → openhtmltopdf 1.1.87 (PDFBox 3) | Szablon HTML/CSS łatwy do zmiany; czysta Java, bez przeglądarki |
| Czcionka DejaVu Sans osadzona w aplikacji (`resources/fonts/documents`, licencja obok) | Polskie znaki niezależnie od serwera; wygląd do dyskusji |
| Zdjęcia zmniejszane do 1600 px (dłuższy bok), JPEG 85% | Brak miniatur na serwerze; jakość druku do weryfikacji |
| Brak obrotu zdjęć wg EXIF | Zdjęcia z telefonu zapisane „bokiem” mogą wyjść obrócone (w przeglądarce wyglądają dobrze) |
| Zdjęcia WebP bez adnotacji nie są osadzane (komunikat w dokumencie) | ImageIO nie czyta WebP bez dodatkowej biblioteki |
| Uszkodzony plik zdjęcia → zdjęcie pominięte, podpis zostaje, dokument powstaje | Log ostrzeżenia na serwerze |
| Generowanie synchroniczne w żądaniu HTTP, cały PDF w pamięci | Przy setkach zdjęć może trwać długo — DO SPRAWDZENIA |
| Daty w strefie Europe/Warsaw, liczby z polskim przecinkiem, braki jako „—” | — |
| Kolejność drzew, prac i zdjęć = kolejność dodania | Brak sortowania po numerze drzewa (numery są tekstem) |
| Placeholdery tylko w szablonie (bez nowych pól domenowych) | Kursywa, kolor — nigdy fikcyjne dane |
| A4, marginesy 20/18 mm, numer strony „Strona N z M”, stopka „ArboMachine - Dokument projektu (wersja MVP)” | — |

---

## Import, eksport i QGIS — etapy 23–24

**Status:** V1 IMPLEMENTED / DISCOVERY REQUIRED. Szczegóły: `docs/discovery/etap-23-import-export.md`,
`docs/discovery/etap-24-qgis.md`, instrukcja użytkownika: `docs/integrations/qgis.md`.

| Temat | Stan |
|---|---|
| Formaty wymagane przez klientów | OPEN DOMAIN QUESTION: jakie pliki arborysta dostaje i oddaje (od projektantów, geodetów, zarządców)? |
| Formaty wymagane przez urzędy | OPEN DOMAIN QUESTION: czy urzędy wymagają konkretnych formatów / schematów atrybutów? V1 NIE udaje zgodności z żadnym formatem urzędowym. |
| XLS / XLSX | V1 IMPLEMENTED: eksport i import XLSX (arkusz „Drzewa”, liczby jako liczby). Stary `.xls` nieobsługiwany. OPEN: układ arkuszy używanych dziś przez arborystę. |
| CSV | V1 IMPLEMENTED: `;`, UTF-8 (eksport z BOM), przecinek dziesiętny; import wykrywa `;` `,` tab, wymaga UTF-8. |
| GeoJSON | V1 IMPLEMENTED: eksport (FeatureCollection, Point, WGS 84, schemat V1) i import (Point, CRS84/EPSG:4326). |
| GeoPackage | V1 IMPLEMENTED: eksport `.gpkg` (warstwa `trees`, POINT, EPSG:4326; biblioteka NGA). DEFERRED: import GeoPackage. OPEN: czy arborysta potrzebuje GPKG. |
| SHP | DEFERRED / OPEN: czy legacy SHP jest wymagany (ograniczenia DBF: nazwy pól do 10 znaków, kodowanie, typy). Nazwy GIS są już krótkie. |
| DXF / DWG | DEFERRED / OPEN: CAD (ZWCAD / aplikacja terenowa) — format, warstwy, bloki symboli. |
| QGZ (projekt QGIS) | DEFERRED: generowanie projektu QGIS z warstwami, stylem, podkładem, etykietami, układem wydruku. |
| QML (styl QGIS) | DEFERRED: styl po `map_symbol` / pracach. Symbole eksportowane jako dane (kody), nie jako style. |
| Etykiety, układ wydruku, podkłady w QGIS | OPEN DOMAIN QUESTION: jak wygląda mapa końcowa. |
| Nazwy kolumn | V1 IMPLEMENTED: jedno źródło `TreeExchangeField` — nazwa techniczna (GeoJSON), krótka GIS (GPKG), polska etykieta (CSV/XLSX). Import akceptuje każdą z nich. OPEN: nazwy kolumn w plikach arborysty / aplikacji terenowej. |
| Obowiązkowe pola | V1 IMPLEMENTED: tylko numer inwentaryzacyjny. OPEN: czy import ma wymagać gatunku / lokalizacji. |
| Duplikaty numerów | V1 IMPLEMENTED: każdy wiersz = nowe drzewo; duplikaty (w pliku i z projektem) tylko jako ostrzeżenia. OPEN DOMAIN QUESTION: odrzucić / utworzyć duplikat / zaktualizować / zapytać / scalić? |
| Aktualizacja istniejących drzew | NOT SUPPORTED IN V1. OPEN: strategia identyfikacji (numer? `tree_id`? z kontrolą własności i konfliktów). `treeId` z pliku jest ignorowany. |
| Round-trip QGIS | V1 IMPLEMENTED: QGIS → GeoJSON → import jako NOWE drzewa (sprawdzone przez GDAL, nie w samym QGIS). OPEN: update vs create vs merge. |
| Import częściowy | OPEN: „importuj poprawne wiersze mimo błędów” — V1 wszystko albo nic. |
| CRS | V1 IMPLEMENTED: EPSG:4326 w eksporcie i imporcie (`CoordinateTransformer` jako miejsce na transformację). OPEN: PL-2000 (która strefa?), PL-1992, inne lokalne; wybór układu przy eksporcie; import plików w PL-1992/PL-2000. |
| Jednostki | V1 IMPLEMENTED: cm (obwód), m (wysokość, korona), stopnie dziesiętne. OPEN: czy pliki klientów używają innych jednostek (np. obwód w m, pierśnica zamiast obwodu). |
| Kodowanie | V1 IMPLEMENTED: UTF-8. OPEN: czy trzeba przyjmować Windows-1250 (stare pliki z Excela). |
| Eksport TreeWork | V1 IMPLEMENTED: nazwy prac w kolumnie / atrybucie (`work_names`), w GeoJSON pełna lista `works`. OPEN: osobna tabela prac (jeden wiersz = jedna praca)? Import prac — DEFERRED. |
| Eksport zdjęć | DEFERRED: zdjęcia i adnotacje nie są eksportowane (bez Base64). OPEN: ZIP ze zdjęciami? linki? |
| Import zdjęć | DEFERRED: import zdjęć (ZIP, zewnętrzny aparat). |
| Mapowanie kolumn w UI | DEFERRED: nagłówki muszą odpowiadać schematowi. |
| Synchronizacja / WFS / WMS | DEFERRED. |

### Założenia techniczne V1 (bez potwierdzenia)
| Założenie | Uwagi |
|---|---|
| Limity importu: plik do 5 MB, do 2000 drzew | Ochrona serwera; do weryfikacji na realnych projektach |
| Plik importu wysyłany dwa razy („Sprawdź” i „Importuj”), bez stanu na serwerze | Prosto i bezpiecznie; przy dużych plikach podwójny transfer |
| Biblioteki: Commons CSV, fastexcel (XLSX), NGA GeoPackage (sqlite-jdbc z natywnym SQLite, ~16 MB) | GeoPackage tworzony w pliku tymczasowym (zawsze usuwany) |
| Liczby w CSV z przecinkiem dziesiętnym | Wygodne w polskim Excelu; QGIS wymaga ustawienia „przecinek dziesiętny” przy wczytywaniu CSV |
| Eksport bez symbolu drzewa w CSV/XLSX (tylko w GIS) | Symbol to dane prezentacji mapy |
| Nazwa warstwy `trees` i pola GIS po angielsku | Stabilne nazwy techniczne; UI po polsku |
| Kolejność drzew w eksporcie = kolejność dodania | — |

### Niesprawdzone
- **Otwarcie plików w prawdziwym QGIS** — sprawdzone tylko GDAL 3.12.4 (ten sam silnik odczytu); checklista
  ręcznego testu w `docs/integrations/qgis.md`.
- Pliki zapisane przez prawdziwy QGIS (inne wersje GDAL, opcje eksportu).
- Duże projekty (tysiące drzew), pliki od klientów w innych układach.

---

## GNSS / RTK — etap 25

**Status:** V1 IMPLEMENTED / DISCOVERY REQUIRED. Szczegóły: `docs/discovery/etap-25-gnss.md`.
Uzupełnia wcześniejsze pytania z sekcji „Lokalizacja drzewa — dane pomiaru”.

| Temat | Stan |
|---|---|
| Źródło pozycji | V1 IMPLEMENTED: GPS urządzenia przez Geolocation API przeglądarki (jeden pomiar, wysoka dokładność, 30 s). |
| Dane pomiaru | V1 IMPLEMENTED: dokładność pozioma, wysokość n.p.m., źródło (`MANUAL`/`MAP`/`DEVICE_GNSS`/`IMPORT`). OPEN: czy zapisywać czas pomiaru, PDOP/HDOP, liczbę satelitów, typ rozwiązania (FIX/FLOAT), wysokość anteny? |
| RTK / NTRIP | DEFERRED / OPEN: czy projekty wymagają centymetrowej dokładności? Jaki dostawca korekt (ASG-EUPOS, komercyjny)? Kto płaci za subskrypcję? |
| Odbiorniki Bluetooth | Etap 31: V1 przez pozorowaną lokalizację Androida (aplikacja odbiornika podaje pozycję systemowi, przeglądarka czyta ją Geolocation API) + instrukcja w karcie drzewa. OPEN: model odbiornika arborysty, iOS, odczyt NMEA. |
| Aplikacja natywna | OPEN: czy potrzebna wersja natywna (Android / iOS) z pełnym dostępem do GNSS? |
| Progi dokładności | V1: do 5 m dobra, do 15 m akceptowalna, powyżej słaba (ostrzeżenie, zapis nadal możliwy). OPEN: jaka dokładność jest wymagana w dokumentacji? Czy blokować zapis słabego pomiaru? |
| Uśrednianie pomiaru | Etap 31: V1 IMPLEMENTED — „Uśrednij 10 odczytów” (maks. 20 s) z rozrzutem; nadal dostępny jeden odczyt. |
| Pomiar RTK w UI | OPEN: z tej samej sekcji lokalizacji czy osobny ekran pomiarów? |
| Mapa offline / podkład | OPEN: czy mapa offline ma mieć podkład (kafelki OSM nie mogą być masowo cache'owane - polityka OSM)? Ortofotomapa? |
| Geokodowanie | OPEN: czy potrzebny adres z pozycji GPS (np. do opisu obiektu)? |
| GPS bez sieci | V1: Geolocation API działa bez internetu (GPS urządzenia); bez GPS (Wi-Fi/sieć) pozycja może być niedostępna. DO SPRAWDZENIA w terenie. |

---

## Praca offline — etap 26

**Status:** V1 IMPLEMENTED / DISCOVERY REQUIRED. Szczegóły: `docs/discovery/etap-26-offline.md`.

| Temat | Stan |
|---|---|
| Zakres danych offline | V1 IMPLEMENTED: odczyt ostatnio pobranych danych (projekty, drzewa, prace, mapa bez podkładu); zapis: dodanie i edycja drzewa, lokalizacja, dodanie i edycja pracy. OPEN: czy offline potrzebne są zdjęcia (pliki z aparatu w kolejce), adnotacje, symbole, usuwanie? |
| Zdjęcia offline | DEFERRED / OPEN: przechowywanie zdjęć w IndexedDB i wysyłka po połączeniu (rozmiar pamięci tabletu, 15 MB na zdjęcie). |
| Konflikty | V1: „ostatni zapis wygrywa” (PATCH z kompletem pól) albo zatrzymanie synchronizacji przy błędzie serwera. OPEN: kto rozstrzyga konflikty, gdy w przyszłości kilka osób pracuje na jednym projekcie? |
| PWA | V1: tylko service worker powłoki aplikacji (otwarcie strony bez sieci). OPEN: pełne PWA (manifest, instalacja na ekranie głównym, ikony)? |
| Granularność synchronizacji | V1: pojedyncze żądania FIFO istniejącymi endpointami. OPEN: endpoint wsadowy / synchronizacja całego projektu? |
| Informacja w UI | V1: pasek stanu, liczba czekających zmian, oznaczenia „Czeka na synchronizację”, błąd z „Spróbuj ponownie” / „Pomiń”. OPEN: czy potrzebna lista wszystkich operacji offline? |
| Pobieranie projektu „na wyjazd” | OPEN: V1 ma tylko to, co użytkownik otworzył online. Czy potrzebny przycisk „Pobierz projekt do pracy offline” (wszystkie drzewa, prace, zdjęcia)? |
| Duplikaty po zerwanym połączeniu | DŁUG: brak kluczy idempotencji - żądanie, które dotarło do serwera bez odpowiedzi, może przy synchronizacji utworzyć duplikat. |
| Nakładanie zmian na dane z serwera | DŁUG: po powrocie sieci, gdy zmiany wciąż czekają (np. po błędzie), ekrany pokazują stan serwera do czasu synchronizacji. |
| Bezpieczeństwo danych na urządzeniu | V1: kopie odczytów kasowane przy wylogowaniu; niewysłane zmiany zostają (tylko to samo konto je wyśle). OPEN: szyfrowanie danych na urządzeniu? limit czasu przechowywania? co z tabletem zgubionym w terenie? |
| Testy terenowe | DO SPRAWDZENIA: słaby zasięg, przełączanie sieci, uśpienie tabletu, długa praca bez sieci (setki zmian), Safari na iPadzie (IndexedDB i service worker). |

### Założenia techniczne V1 (bez potwierdzenia)
| Założenie | Uwagi |
|---|---|
| IndexedDB bez biblioteki (własny mały wrapper) | Dexie / localForage niepotrzebne przy 3 magazynach |
| Tymczasowe ujemne id obiektów utworzonych offline | Adres karty `/trees/-…`; po synchronizacji przekierowanie |
| Ponawianie synchronizacji co 30 s przy niepustej kolejce | Oprócz zdarzenia `online` i udanych odpowiedzi serwera |
| Kolejka niepusta → nowe zmiany też idą do kolejki (nawet online) | Zachowanie kolejności |
| Brak limitu wieku i rozmiaru kopii odczytów | Kopie kasowane przy wylogowaniu |
| Service worker tylko w zbudowanej aplikacji i w bezpiecznym kontekście | Produkcja wymaga HTTPS |

---

## Waloryzacja drzew — etap 27

**Status:** V1 IMPLEMENTED (zapis oceny arborysty) / DISCOVERY REQUIRED. Szczegóły: `docs/discovery/etap-27-waloryzacja.md`.
**Aplikacja niczego nie wylicza** — brak wzorów, punktów, wag, współczynników i kwot PLN (pierwsza wersja polecenia
proponowała wzór „obwód² × wysokość” i wycenę w PLN; obowiązująca wersja tego zabrania, bo metoda nie jest znana).

| Temat | Stan |
|---|---|
| Metoda waloryzacji | OPEN DOMAIN QUESTION: opisowa, punktowa, klasowa, mieszana? Kto i jak ją ustala? |
| Skale wartości składowych | V1: wolny tekst (wiekowa, przyrodnicza, ekologiczna, kulturowa, krajobrazowa). OPEN: czy są klasy / skale (np. I–V)? |
| Ocena ogólna | V1 HYPOTHESIS: opcjonalna kategoria Niska / Średnia / Wysoka / Bardzo wysoka, brak = „Nie oceniono” — **nie jest to skala branżowa**, wybierana ręcznie. OPEN: czy wynik końcowy istnieje i jak się nazywa? |
| Punkty i wagi | OPEN: czy stosowane? V1 ich nie ma. |
| Wycena pieniężna | DEFERRED / OPEN: czy potrzebna, jaką metodą (odtworzeniowa, tablice, przepisy o opłatach za usunięcie drzew), kto ją podpisuje? V1 nie pokazuje żadnych kwot. |
| Przechowywanie | V1 IMPLEMENTED: ocena zapisana w bazie (to dane wpisane przez arborystę, nie wynik obliczeń). OPEN: czy potrzebna historia ocen (kolejne inwentaryzacje, porównania)? |
| Odszkodowania / historia wartości | OPEN: czy klient potrzebuje historii wartości drzewa lub odszkodowań? |
| Grupy drzew | OPEN: czy waloryzuje się też grupy, szpalery, zadrzewienia (V1: pojedyncze drzewo)? |
| Waloryzacja a rodzaj opracowania | OPEN: czy pola zależą od rodzaju dokumentu (waloryzacja, inwentaryzacja, program prac)? |
| Mapa | V1 IMPLEMENTED: tryb „Waloryzacja” z umownymi kolorami (jasny żółty → ciemna zieleń, szary = nie oceniono). OPEN: jakie kolory / oznaczenia klas stosuje arborysta? |
| Dokument | V1 IMPLEMENTED: kolumna „Waloryzacja” w zestawieniu, blok „Waloryzacja” w karcie drzewa, „Nie przeprowadzono waloryzacji.”, liczba drzew z waloryzacją. OPEN: jak waloryzacja wygląda w finalnym dokumencie? |
| Eksport | V1 IMPLEMENTED: CSV/XLSX — kategoria i wszystkie opisy; GeoJSON/GeoPackage — tylko kategoria (`valuationCategory` / `val_categ`). OPEN: czy opisy są potrzebne w GIS? |
| Import waloryzacji | DEFERRED: import plików ignoruje pola waloryzacji. |
| DOCX z edytowalną waloryzacją | DEFERRED. |
| Oficjalne tablice / przeliczniki | OPEN: czy istnieją i czy integrować? |

### Założenia techniczne V1 (bez potwierdzenia)
| Założenie | Uwagi |
|---|---|
| Osobna encja `TreeValuation` 1:1 z `Tree` (tabela `tree_valuation`, `tree_id` UNIQUE) | Inwentaryzacja (pomiary) oddzielona od oceny eksperckiej |
| `GET …/valuation` zwraca 200 z `hasValuation: false` zamiast 404 | Jeden kształt odpowiedzi dla karty drzewa i kopii offline |
| Pusta ocena nie może być zapisana (400) — do usunięcia służy DELETE | — |
| `PUT` zastępuje całą ocenę (pominięte pole = brak wartości); `DELETE` idempotentny | Bezpieczne powtórzenie przy synchronizacji offline |
| Limit 5000 znaków na opis | Techniczny, jak opis pracy |
| Kolory kategorii: `#f2d16b`, `#8fcf6b`, `#2f9e44`, `#14532d`, szary `#9aa3a0` | Umowne, różna jasność (część osób z zaburzeniami widzenia barw) |
| Filtr listy „Nieocenione” = brak waloryzacji albo waloryzacja bez oceny ogólnej | — |
| Waloryzacja w kolejce offline: `UPSERT_TREE_VALUATION`, `DELETE_TREE_VALUATION` | Ten sam mechanizm co etap 26; konflikty: ostatni zapis wygrywa |
| Waloryzacja nie wpływa automatycznie na nic (prace, priorytety, symbole) | Decyzje podejmuje arborysta |

---

## Gospodarka drzewostanem — etap 28

**Status:** V1 IMPLEMENTED (rozszerzenie `TreeWork`) / DISCOVERY REQUIRED. Szczegóły:
`docs/discovery/etap-28-gospodarka-drzewostanem.md`. Bez osobnego modelu `ManagementTask` (pierwsza wersja polecenia
go proponowała; obowiązująca zabrania równoległego modelu obok `TreeWork`).

| Temat | Stan |
|---|---|
| Kategorie prac | OPEN DOMAIN QUESTION: rzeczywista lista prac i kody (V1: nazwa = wolny tekst, jak od etapu 14). |
| Status | V1 HYPOTHESIS: Zalecenie / Zaplanowana / Wykonana / Anulowana, domyślnie Zalecenie, **dowolne przejścia**. OPEN: czy są inne statusy („w trakcie”, „odebrana”, „odrzucona”) i reguły przejść? Kto zmienia status? |
| Priorytet | V1 HYPOTHESIS: Niski / Normalny / Wysoki / Pilny, opcjonalny, ustawiany ręcznie. **Priorytet organizacyjny, nie standard arborystyczny.** OPEN: czy potrzebny, jaką skalę stosuje arborysta? |
| Termin | V1: opcjonalna data, **może być w przeszłości** (projekty historyczne). OPEN: czy planuje się datą, sezonem, rokiem, horyzontem („do 1 roku”)? Okres lęgowy? |
| Prace opóźnione | V1: napis „(po terminie)” przy pracy do wykonania z terminem przed dziś — tylko informacja. OPEN: czy wyróżniać / filtrować? |
| Data wykonania | V1: opcjonalna; „Oznacz jako wykonaną” wpisuje dzisiejszą datę, jeśli brak. Wybór statusu „Wykonana” w formularzu podpowiada dzisiejszą datę. OPEN: czy wymagać daty przy „Wykonana”? |
| Uzasadnienie | V1: istniejące pole „Opis” (bez osobnego `justification`). OPEN: czy opis i uzasadnienie to różne rzeczy? |
| Grupy drzew / prace bez drzewa | OPEN: czy praca może dotyczyć szpaleru, grupy, zadrzewienia, czy są prace projektowe (np. nasadzenia zastępcze)? V1: praca zawsze przy jednym drzewie. |
| Realizacja | OPEN: czy wykonanie jest śledzone w ArboMachine (kto wykonał, protokół, zdjęcia „po”, załączniki)? Koszty? Wykonawcy? |
| Kontrole i prace cykliczne | OPEN: daty kontroli, prace powtarzalne. V1: brak. |
| Waloryzacja a gospodarka | V1: **brak automatyki** — waloryzacja nie tworzy prac ani priorytetów. |
| Mapa | V1 IMPLEMENTED: tryb „Gospodarka drzewostanem”: czerwony `#c92a2a` = pilna praca do wykonania, niebieski `#1971c2` = prace do wykonania, ciemnoszary `#495057` = tylko wykonane/anulowane, jasnoszary `#ced4da` = bez prac; znaczki tylko prac do wykonania. Kolory umowne. OPEN: jak arborysta oznacza stan prac na mapie? |
| Widok projektu | V1 IMPLEMENTED: zakładka „Gospodarka drzewostanem” — liczniki, filtry (status z „Do wykonania” jako domyślnym, priorytet, drzewo), kolejność: do wykonania → termin → priorytet; edycja w karcie drzewa, w widoku tylko „Oznacz jako wykonaną”. |
| Dokument | V1 IMPLEMENTED: sekcja „5. Gospodarka drzewostanem” (tabela: Nr, Gatunek, Zalecana praca, Status, Priorytet, Termin, Wykonano, Uwagi), status/priorytet/terminy w karcie drzewa, liczniki prac do wykonania i wykonanych. Dokument nadal neutralny („Dokument projektu”), nie „Plan gospodarki drzewostanem”. OPEN: jak prace wyglądają w finalnym opracowaniu? |
| Eksport / QGIS | V1 IMPLEMENTED: XLSX arkusz „Prace”, CSV prac, GeoJSON `works[]` ze statusem/priorytetem/terminami, atrybuty drzewa `openWorkCount`/`works_open` i `nextPlannedWorkDate`/`work_next`. DEFERRED: osobna warstwa prac w GeoPackage, import prac. Status i priorytet eksportowane jako kody (nie polskie nazwy). |
| Konflikty offline | V1: ostatni zapis wygrywa (jak etap 26). OPEN: czy potrzebna ręczna weryfikacja konfliktów? |

### Założenia techniczne V1 (bez potwierdzenia)
| Założenie | Uwagi |
|---|---|
| Nowe kolumny `tree_work.status`, `priority`, `planned_date`, `completed_date` — nullowe | Dodane przez `ddl-auto=update`; `status = null` (prace sprzed etapu 28) czytany jako `RECOMMENDED`, bez migracji danych |
| Status i priorytet zapisywane jako tekst (`EnumType.STRING`) | Zmiana słownika = zmiana enuma, bez przenumerowania |
| PATCH pracy nadal = komplet pól (pominięty status → Zalecenie, pominięty priorytet/termin → wyczyszczony) | Spójne z dotychczasową edycją; „Oznacz jako wykonaną” wysyła komplet pól |
| Brak walidacji kolejności dat (data wykonania może być przed terminem) | Brak reguł biznesowych |
| Filtry `?status=&priority=` w `GET …/works` — wiele wartości = OR, puste = bez filtra; filtr priorytetu pomija prace bez priorytetu | Frontend filtruje lokalnie (działa offline); filtr API dla integracji |
| Usuwanie pracy offline (`DELETE_TREE_WORK`) — praca znika z kopii od razu | UI nie oferuje usuwania pracy utworzonej offline przed synchronizacją |
| Liczniki w PDF: „do wykonania” = Zalecenie + Zaplanowana | — |

---

## Monitoring stanu drzew — etap 29

**Status:** V1 IMPLEMENTED (dziennik kontroli) / DISCOVERY REQUIRED. Szczegóły: `docs/discovery/etap-29-monitoring-i-gis.md`.
To **nie jest ocena VTA ani ocena ryzyka** — tylko zapis, kiedy arborysta sprawdził drzewo i jak ogólnie ocenił stan.

| Temat | Stan |
|---|---|
| Skala oceny | V1 HYPOTHESIS: opcjonalna ocena Dobry / Umiarkowany / Zły, brak = „Nie oceniono”. OPEN: jaką skalę stosuje arborysta (klasy żywotności, klasy ryzyka, skala 1–5)? |
| Karta oceny stanu | OPEN DOMAIN QUESTION: w pliku projektu aplikacji terenowej arborysty jest strukturalna karta: wartość drzewa (pomnik przyrody, gatunek chroniony, ostoja), liczba przewodników 1–10, kształt korony (regularna / asymetryczna: odrosty z pnia, zdeformowana cięciem), odziomek, pień (pochylony, pęknięcia, słabe wiązanie, owocniki, rozkład, pędy regeneracyjne, rany, ubytek powierzchniowy / wgłębny mały / średni / rozległy, kominowy, rozwidlenie V/U), konary, gałęzie (posusz 5–100%, jemioła, gniazda), korzenie, siedlisko, notatki. Czy ArboMachine ma przejąć tę kartę jako słownik? Czy to część kontroli, czy karty drzewa? |
| Uwagi z arkusza | V1: kolumny karty stanu z arkusza aplikacji terenowej łączone przy imporcie w tekst „Stan / uwagi” drzewa (nie w kontrolę). |
| Cykl kontroli | OPEN: czy kontrole są cykliczne (co rok, co 3 lata), czy potrzebny termin następnej kontroli i przypomnienie? V1: brak. |
| Zdjęcia przy kontroli | OPEN: czy kontrola ma własne zdjęcia (porównanie „przed / po”)? V1: zdjęcia tylko przy drzewie. |
| Kto kontroluje | OPEN: zawsze arborysta-właściciel projektu? V1 tak. |
| Ocena „Zły” | V1: zapis kontroli z oceną „Zły” tworzy powiadomienie (i Web Push). OPEN: czy to właściwy wyzwalacz? czy powiadomienie dostaje ktoś jeszcze (zleceniodawca, urząd)? |
| Lista drzew i mapa | V1: plakietka ostatniej oceny, filtr „Stan: Zły / … / Nie sprawdzone”. Mapa nie ma trybu monitoringu. OPEN: czy potrzebny? |
| Dokument | V1 IMPLEMENTED: liczba drzew skontrolowanych i w złym stanie, tabela kontroli w karcie drzewa. |
| Eksport | V1: XLSX arkusz „Monitoring” (wszystkie kontrole), atrybuty drzewa `mon_date` / `mon_status` (GIS) i `lastMonitoringNotes` (tylko CSV/XLSX). DEFERRED: import kontroli. |

### Założenia techniczne V1 (bez potwierdzenia)
| Założenie | Uwagi |
|---|---|
| Encja `TreeMonitoringRecord` (tabela `tree_monitoring_record`, indeks `tree_id, inspection_date`) | Wiele kontroli na drzewo, najnowsza pierwsza |
| Data kontroli wymagana (bez blokady dat przyszłych); ocena i uwagi opcjonalne | Uwagi do 5000 znaków. OPEN: czy blokować daty z przyszłości i puste kontrole? |
| `PUT` zastępuje całą kontrolę, `DELETE` idempotentny | Offline: `CREATE/UPDATE/DELETE_TREE_MONITORING_RECORD` w kolejce etapu 26 |

---

## Grupy drzew i GIS — etap 29

**Status:** V1 IMPLEMENTED / DISCOVERY REQUIRED. Szczegóły: `docs/discovery/etap-29-monitoring-i-gis.md`. W poleceniu
obszary nazywano „porostami” — w interfejsie przyjęto nazwę **„Grupy drzew”** (w kodzie `TreeStand`).

| Temat | Stan |
|---|---|
| Nazwa | V1: „Grupy drzew”. OPEN: czy arboryści mówią „porost”, „zadrzewienie”, „skupina”? Czy to różne typy? |
| Geometria | V1: jeden wielokąt (bez otworów) w WGS 84; rysowany na mapie albo importowany (DXF warstwy `-Area`, Shapefile, GeoPackage, GeoJSON; MultiPolygon = kilka grup). OPEN: czy grupa bywa linią (szpaler) albo kilkoma obszarami? |
| Drzewa w grupie | V1: przypisanie ręczne i „przypisz drzewa leżące w obszarze”; drzewo może być w kilku grupach. OPEN: czy grupa ma też drzewa nieinwentaryzowane pojedynczo (liczba, gatunki, powierzchnia)? |
| Pomiary grupy | OPEN: czy grupa ma własne pomiary (liczba sztuk, przedział obwodów, zwarcie)? V1: tylko pole powierzchni liczone z obszaru. |
| Prace i waloryzacja grupy | OPEN (patrz etapy 27–28). V1: brak. |
| Symbol iglastych | V1: gwiazdka = gatunek iglasty (rozpoznany po nazwie łacińskiej/polskiej), koło = liściasty / nieznany. OPEN: czy arborysta chce takiego rozróżnienia i czy z innymi kształtami (krzewy, martwe drzewa)? |
| Rysunek DXF | V1: punkty drzew z warstw `-Point`, numery z warstw `-Label`, obszary z `-Area` (konwencja aplikacji terenowej arborysty). Okręgi pnia i korony **nie są importowane**. OPEN: czy importować wysokość / średnicę korony z rysunku? jakie nazwy warstw mają inne programy? |
| Układ współrzędnych | V1: PL-2000 strefy 5–8 i PL-1992 rozpoznane z `.prj` / SRS albo z zakresu liczb, przeliczone do WGS 84 (ostrzeżenie w imporcie). OPEN: czy potrzebny eksport w PL-2000 (dla projektantów, urzędów)? |
| Arkusz aplikacji terenowej | V1: rozpoznawane nagłówki (Etykieta, Gatunki, Obwody na 130 cm, Pozycja WGS84, Karta drzewa…); wiersze obszarów pomijane; obwody wielopniowe („57+34”) trafiają do uwag, nie są sumowane. OPEN: jak liczyć obwód drzewa wielopniowego? |
| Plik projektu aplikacji terenowej | DEFERRED: format własny (binarny) — nie importujemy. OPEN: czy arboryści mogą eksportować do XLSX/DXF (tak robią dziś)? |

### Założenia techniczne V1 (bez potwierdzenia)
| Założenie | Uwagi |
|---|---|
| Geometria jako tekst GeoJSON w `tree_stand.geometry_geojson`, bez PostGIS | Punkt-w-wielokącie i pole liczone w Javie |
| Import obszarów: najwyżej 500, limit pliku 15 MB, „wszystko albo nic” | Jak import drzew |
| Shapefile: ZIP z `.shp`, `.dbf`, opcjonalnie `.prj` i `.cpg`; kodowanie DBF z `.cpg`, potem UTF-8, w ostateczności Windows-1250 | — |
| Eksport Shapefile: ZIP z warstwami `trees` (punkty) i `stands` (wielokąty), WGS 84, UTF-8; nazwy pól DBF ≤ 10 znaków | Teksty dłuższe niż 254 znaki obcinane (ograniczenie formatu) |

---

## Dane zlecenia projektu — etap 29

| Temat | Stan |
|---|---|
| Pola | V1 IMPLEMENTED: zleceniodawca, adres obiektu, data zlecenia — opcjonalne, w nagłówku projektu, PDF i XLSX. OPEN: czy potrzebne: numer umowy, wykonawca/autor, termin oddania, dane kontaktowe, działki ewidencyjne? |
| Zleceniodawca jako obiekt | OPEN: słownik zleceniodawców (ten sam urząd w wielu projektach)? V1: tekst. |

---

## Powiadomienia i Web Push — etap 30

**Status:** V1 IMPLEMENTED. Szczegóły: `docs/discovery/etap-30-integracje-i-finalizacja.md`.

| Temat | Stan |
|---|---|
| Kanały | V1: lista w aplikacji (Integracje, Mój profil) + Web Push w przeglądarce. OPEN: e-mail? SMS? |
| Zdarzenia | V1: kontrola z oceną „Zły”; codziennie o 7:00 — prace z terminem na jutro (jedno zbiorcze powiadomienie); powiadomienie testowe. OPEN: jakie zdarzenia są naprawdę potrzebne (prace po terminie, zbliżająca się kontrola, sezon lęgowy)? |
| Godzina przypomnienia | V1: 7:00 czasu polskiego, dla wszystkich. OPEN: ustawienia per użytkownik? |
| Odbiorcy | V1: tylko właściciel projektu. OPEN: zespół, zleceniodawca, urząd. |
| Urządzenia | V1: każde urządzenie (przeglądarka) włącza powiadomienia osobno; nieaktualne subskrypcje usuwane automatycznie. iPhone/iPad: tylko aplikacja dodana do ekranu głównego. |

### Założenia techniczne V1 (bez potwierdzenia)
| Założenie | Uwagi |
|---|---|
| Web Push bez zewnętrznej biblioteki: szyfrowanie RFC 8291 (aes128gcm), VAPID RFC 8292 (ES256) | Zweryfikowane wektorem testowym z RFC 8291 |
| Wysyłka tylko do usług push na liście dozwolonych hostów, tylko HTTPS | Ochrona przed SSRF (adres subskrypcji podaje przeglądarka/klient) |
| Klucze VAPID z konfiguracji; brak = wygenerowane i zapisane w bazie | Zmiana kluczy = wszystkie urządzenia muszą włączyć powiadomienia ponownie |
| Deduplikacja: unikalny klucz (użytkownik + zdarzenie), np. `works-due:<data>` | Jedno przypomnienie dziennie |
| Treść powiadomienia: numer drzewa, nazwa projektu, data kontroli, link do karty drzewa | Szyfrowana end-to-end (usługa push nie widzi treści) |

---

## Kalendarz ICS — etap 30

| Temat | Stan |
|---|---|
| Zakres | V1: prace do wykonania (Zalecenie, Zaplanowana) z terminem → wydarzenia całodniowe; numer drzewa, gatunek, priorytet, opis w treści. OPEN: czy także kontrole, prace wykonane, prace bez terminu? |
| Aktualizacja | V1: jednorazowy plik (ponowny import aktualizuje wydarzenia po stałym UID). OPEN: subskrypcja kalendarza przez adres URL (wymaga tokenu w adresie)? |
| Import z kalendarza | DEFERRED: brak. |

---

## Wydanie MVP — etap 30

| Temat | Stan |
|---|---|
| Hosting | OPEN: gdzie działa produkcja (VPS, chmura), domena, certyfikat HTTPS, kto administruje? |
| Kopie zapasowe | OPEN: częstotliwość i miejsce kopii bazy i katalogu zdjęć. V1: procedura opisana w `docs/WYDANIE.md`. |
| Wersjonowanie | V1: tag Git `vX.Y.Z` + JAR z sumą SHA-256 (`scripts/build-release.*`). |
| Dane demonstracyjne | V1: `scripts/demo/seed-demo-project.mjs` (przykładowy park albo prawdziwe pliki arborysty). |
| RODO / regulamin | OPEN: polityka prywatności, umowa powierzenia (zdjęcia, lokalizacje, dane zleceniodawców). |

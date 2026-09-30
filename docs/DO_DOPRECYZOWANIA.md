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
7. Automatyczne przypisanie zdjęcia do Tree (jak w ArboMap) — na jakiej podstawie?
8. Offline — czy zdjęcia muszą być robione i kolejkowane bez internetu?
9. Storage produkcyjny — dysk serwera, S3/MinIO, kopie zapasowe, retencja?
10. Sposób użycia zdjęć w dokumentacji (rozmiar, kadrowanie, miniatury, adnotacje).

### Dlaczego wymaga doprecyzowania
Brak realnego workflow fotograficznego arborysty (aparat vs tablet, ArboMap, ZWCAD, dokument końcowy).

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
7. Czy potrzebne są nagrania audio jako załączniki (ArboMap pracuje z nagraniami).
8. Czy dyktowanie w opisie prac (TreeWork) jest potrzebne (V1: jest).
9. Interpunkcja i komendy głosowe („kropka”, „nowa linia”).

---

## Rejestr braków — stan po etapie 18

Jedno miejsce na **wszystko, czego brakuje**: funkcje, niesprawdzone obszary, dług techniczny i sprawy operacyjne.
Aktualizowany po każdym etapie (dopisujemy nowe braki, rozwiązane oznaczamy). Statusy: **OPEN** — decyzja/discovery,
**BRAK** — znana luka funkcjonalna, **DŁUG** — dług techniczny, **DO SPRAWDZENIA** — działa, ale nie zweryfikowane
w realnych warunkach, **OPERACYJNE** — sprawy środowiska.

### Funkcje produktu

| Obszar | Czego brakuje | Status |
|---|---|---|
| Drzewa | Usuwanie drzewa (co ze zdjęciami-plikami i pracami) | BRAK |
| Projekty | Usuwanie / archiwizacja projektu; konto z projektami nie może być usunięte (409 — polityka tymczasowa) | BRAK / OPEN |
| Drzewa | Wyszukiwanie, sortowanie i filtrowanie listy drzew (np. „bez lokalizacji”, „bez zdjęć”) | BRAK |
| Listy | Paginacja list projektów, drzew, zdjęć i prac (dziś zawsze całe listy) | DŁUG |
| Drzewa | Nawigacja „Poprzednie / Następne drzewo”, „Zapisz i dodaj następne” w jednym kroku (jest tylko „Dodaj następne drzewo”) | OPEN |
| Drzewa | Autonumeracja i unikalność numeru drzewa w projekcie | OPEN |
| Drzewa | Słownik gatunków, autocomplete, PL ↔ LAT | BRAK (V1 = wolny tekst) |
| Drzewa | Model wielopniowości, historia pomiarów, strukturalna ocena stanu / VTA / ryzyko | OPEN |
| Formularz | Blokada opuszczenia strony z niezapisanymi zmianami (jest tylko komunikat „Masz niezapisane zmiany”) | BRAK |
| Zdjęcia | Miniatury po stronie serwera / kompresja (galeria ładuje pełne pliki) | BRAK |
| Zdjęcia | Podpisy, kategorie, zdjęcie główne, kolejność, EXIF, adnotacje (szkice cięć), HEIC | OPEN |
| Zdjęcia | Zewnętrzny aparat i automatyczne przypisanie zdjęcia do drzewa | OPEN |
| Zdjęcia | Widok wszystkich zdjęć projektu (zakładka „Zdjęcia i dokumentacja” usunięta w etapie 16) | OPEN |
| Prace | Słownik typów prac, kody, priorytet, termin, status wykonania, parametry | OPEN |
| Mapa | Symbole prac, kolory, legenda, etykiety numerów drzew przy markerach | BRAK / OPEN |
| Mapa | Klastrowanie przy dużej liczbie drzew; korony, strefy ochronne, poligony | BRAK / OPEN |
| Mapa | WMS / Geoportal / ortofotomapa; układy PL-2000 / PL-1992; PostGIS | OPEN |
| Mapa | Start mapy z obszaru/adresu projektu (dziś: widok Polski) | OPEN |
| Podsumowanie | Dodatkowe liczby (drzewa ze zdjęciami, bez prac, lista braków do uzupełnienia) | OPEN |
| Dokumentacja | Generator dokumentów (PDF/DOCX), tabele zaleceń, mapy do raportu | BRAK |
| Import/eksport | XLSX, CSV, GeoPackage, SHP, GeoJSON, DXF, QGIS | BRAK |
| Teren | GNSS/RTK, NTRIP, geolokalizacja urządzenia, praca offline, synchronizacja | OPEN |
| Dyktowanie | Dyktowanie danych strukturalnych, nazw łacińskich, komendy głosowe, offline, nagrania audio | OPEN |
| Konto | Zmiana hasła z poziomu „Mój profil” (dziś tylko przez „Nie pamiętasz hasła?”) | BRAK |
| Konto | Po „Wyloguj” kolejne logowanie w tej samej karcie wraca na ostatnią stronę panelu (np. `/projects`) | OPEN |
| Role | Panel urzędnika — tylko pulpit z zapowiedziami modułów; brak workflow urząd ↔ wykonawca, organizacji | BRAK / FUTURE |
| Współpraca | Wielu użytkowników w jednym projekcie (ProjectMembership) | FUTURE |
| Język | Wielojęzyczność (i18n) — dziś tylko polski UI | OPEN |

### Niesprawdzone w realnych warunkach

| Obszar | Czego nie sprawdzono | Status |
|---|---|---|
| Dyktowanie | Prawdziwy głos i prawdziwy mikrofon — testy E2E używały podstawionego SpeechRecognition | DO SPRAWDZENIA |
| Dyktowanie | Docelowe tablety/przeglądarki (Safari na iPadzie, Chrome na Androidzie), jakość polskiego rozpoznawania w terenie | DO SPRAWDZENIA |
| Dyktowanie | Prywatność: Chrome rozpoznaje mowę przez usługę Google (wymaga internetu, dźwięk opuszcza urządzenie przez przeglądarkę) | OPEN |
| Tablet | Obsługa dotykiem na fizycznym tablecie (testy tylko w emulowanych rozmiarach ekranu) | DO SPRAWDZENIA |
| Zdjęcia | Aparat tabletu przez `<input type="file" accept="image/*">`, duże zdjęcia (8–15 MB) na słabym łączu | DO SPRAWDZENIA |
| Mapa | Wydajność przy setkach/tysiącach drzew | DO SPRAWDZENIA |
| Maile | Wysyłka maili rejestracji/resetu przez prawdziwy SMTP (lokalnie potwierdzanie maila jest wyłączalne) | DO SPRAWDZENIA |

### Dług techniczny

| Obszar | Opis | Status |
|---|---|---|
| Baza | Brak Flyway/Liquibase — schemat aktualizuje `ddl-auto=update` (nowe kolumny tekstowe z etapu 17 dodał Hibernate) | DŁUG |
| Testy backendu | Testy działają na H2, nie na PostgreSQL (błąd tylko-PostgreSQL wyszedł już w etapie 2); rozważyć Testcontainers | DŁUG |
| Testy frontendu | Tylko testy logiki (`node --test`); brak testów renderowania komponentów React | DŁUG |
| Testy E2E | Scenariusze E2E (headless Chrome przez CDP) istnieją tylko lokalnie poza repozytorium — brak E2E w repo i CI | DŁUG |
| Jakość | Brak lintera (ESLint) i CI uruchamiającego testy przy commitach | DŁUG |
| Frontend | Paczka JS ~650 kB (głównie OpenLayers) — brak podziału na części (lazy loading mapy) | DŁUG |
| Bezpieczeństwo | Token JWT w `localStorage`, brak odświeżania tokenu (sesja wygasa po czasie ważności) | OPEN |
| Zdjęcia | Brak zadania sprzątającego osierocone pliki (plik bez rekordu po nieudanym usunięciu) | DŁUG |
| Zdjęcia | Storage produkcyjny i kopie zapasowe plików (dziś katalog na dysku serwera) | OPEN |
| Mapa | Publiczne kafelki OpenStreetMap — polityka użycia nie dopuszcza ruchu produkcyjnego | OPEN |

### Sprawy operacyjne (środowisko lokalne)

| Opis | Status |
|---|---|
| W bazie deweloperskiej zostały konta i projekty testowe z adresami `...@example.com` (z testów E2E etapów 11–18) — do usunięcia przy okazji | OPERACYJNE |
| Katalog `storage/tree-photos` może zawierać pliki testowe z E2E (w `.gitignore`) | OPERACYJNE |
| Lokalne, niezacommitowane zmiany właściciela repo: `application.properties` (port 8083), `tsconfig.tsbuildinfo` | OPERACYJNE |

# Etap 25 — GNSS (GPS w terenie)

**Status:** V1 zaimplementowane — GPS urządzenia przez przeglądarkowe Geolocation API. Zewnętrzne odbiorniki,
RTK / NTRIP i aplikacja natywna: odłożone (pytania w `docs/DO_DOPRECYZOWANIA.md`, sekcja „GNSS / RTK”).

## Ocena opcji

| Opcja | Zalety | Wady | Decyzja V1 |
|---|---|---|---|
| Geolocation API przeglądarki | działa w każdej nowoczesnej przeglądarce, bez sprzętu i bibliotek; na tablecie z GPS ok. 3–10 m | dokładność zależy od urządzenia i nieba; bez GPS — pozycja z Wi-Fi/sieci (dziesiątki–setki m); wymaga HTTPS i zgody | **TAK — jedyne źródło V1** |
| Zewnętrzny odbiornik Bluetooth (Web Bluetooth) | lepsza dokładność | eksperymentalne API, tylko Chrome, każdy odbiornik ma inny protokół (NMEA przez BLE) | NIE — przycisk-zaślepka „Połącz odbiornik GNSS (Bluetooth)” z opisem |
| RTK / NTRIP | centymetry | serwer korekt, subskrypcja (np. ASG-EUPOS), odbiornik RTK; NTRIP w przeglądarce praktycznie niemożliwy | NIE — DO_DOPRECYZOWANIA |
| Aplikacja natywna / PWA z kodem natywnym | pełny dostęp do GNSS (PDOP, satelity, surowe dane) | osobna aplikacja | NIE — poza MVP |

## Model danych

`TreeLocation` (kolumny tabeli `tree`, wszystkie nullowe, bez nowej tabeli):

| Pole | Kolumna | Typ | Znaczenie |
|---|---|---|---|
| `latitude`, `longitude` | jak dotąd | numeric(10,7) | WGS 84 |
| `altitudeM` | `location_altitude_m` | numeric(7,2) | wysokość n.p.m. z GNSS (zakres −500…9000 m) |
| `accuracyM` | `location_accuracy_m` | numeric(7,2) | dokładność pozioma (promień, ok. 95% ufności jak w Geolocation API; 0…10000 m) |
| `source` | `location_source` | varchar(32) | `MANUAL`, `MAP`, `DEVICE_GNSS`, `IMPORT`; null = punkt sprzed etapu 25 |

Bez czasu pomiaru, PDOP, liczby satelitów, typu rozwiązania RTK, wysokości anteny — do potwierdzenia.

## API

Bez nowych endpointów: istniejący `PUT /api/projects/{projectId}/trees/{treeId}/location` przyjmuje dodatkowo
opcjonalne `altitudeM`, `accuracyM`, `source` (`MANUAL|MAP|DEVICE_GNSS`; `IMPORT` ustawia tylko import plików).
Walidacja zakresów (`@DecimalMin/@DecimalMax`, `@Pattern`), własność projektu → drzewa jak dotąd (404 dla cudzego).
`TreeLocationResponse` zwraca nowe pola. Zmiana samego pomiaru tego samego punktu też przesuwa `updatedAt`.

## UI

Karta drzewa → „Lokalizacja” → **„Użyj GPS urządzenia”**: jeden pomiar (`getCurrentPosition`,
`enableHighAccuracy`, 30 s, bez wyniku z pamięci), „Pobieranie lokalizacji…”, podgląd punktu na mapie, dokładność
z oceną (do 5 m dobra, do 15 m akceptowalna, powyżej słaba + ostrzeżenie), wysokość, „Zapisz lokalizację z GPS”,
„Zmierz ponownie”, „Anuluj”. Polskie komunikaty: brak zgody, brak pozycji, przekroczony czas, brak HTTPS, brak API.
Zoom mapy się nie zmienia. Zapisany punkt pokazuje opis pomiaru („GPS urządzenia, dokładność ±4,2 m”).
Mapa i formularz ręczny zapisują źródło `MAP` / `MANUAL`. Dane pomiaru trafiają też do eksportów
(CSV/XLSX/GeoJSON/GeoPackage: dokładność, wysokość, źródło) i do dokumentu PDF („Pomiar lokalizacji”).

## Testy

- Backend (`TreeLocationGnssApiIntegrationTest`): zapis i zaokrąglenie pomiaru, źródła MAP/MANUAL, stare
  klienty bez pomiaru, zmiana samego pomiaru, odrzucenie złej dokładności / wysokości / źródła / tekstu zamiast
  liczby, własność (inny użytkownik → 404, bez logowania → 401/403), sekwencja synchronizacji offline.
- Frontend (`deviceGnss.test.ts`, podstawione Geolocation API): zaokrąglenia, opcje pomiaru, polskie komunikaty
  błędów, brak HTTPS / API, ocena dokładności, opis pomiaru.
- E2E (headless Chrome, emulowana geolokalizacja): pomiar ±4,2 m i zapis (źródło `DEVICE_GNSS` w bazie),
  słaba dokładność ±85 m z ostrzeżeniem, odmowa zgody, ekran 390 px bez poziomego przewijania, GPS bez sieci.

**Nie sprawdzono na prawdziwym urządzeniu z GPS w terenie** (dokładność pod koronami drzew, czas ustalania pozycji).

# Etap 26 — praca offline i synchronizacja

**Status:** V1 zaimplementowane w warstwie frontendu (bez zmian w API). Decyzje i pytania:
`docs/DO_DOPRECYZOWANIA.md`, sekcja „Praca offline”.

## Architektura V1

```
ekrany (treesApi / treeWorksApi)
   │  odczyt GET                         │  zapis (drzewo, lokalizacja, praca)
   ▼                                     ▼
httpClient ── kopia odpowiedzi ──► IndexedDB  ◄── offlineService.mutate
   │  bez sieci: ostatnia kopia          │        online i pusta kolejka → zwykły request
   ▼                                     │        offline / kolejka niepusta / tymczasowe id → kolejka
serwer ◄──────── syncPendingChanges (FIFO, te same endpointy) ◄──┘
```

- **IndexedDB** (`arbomachine-offline`): `responses` — kopie odpowiedzi GET (klucz = ścieżka API),
  `pendingChanges` — kolejka zmian (autoIncrement = kolejność), `idMappings` — tymczasowe id → id z serwera.
  Bez biblioteki (Dexie) — mały wrapper `indexedDbOfflineStore.ts`; gdy IndexedDB niedostępne — magazyn w pamięci.
- **Kopie odczytów:** każda odpowiedź GET z tokenem jest zapisywana; bez połączenia `httpClient` zwraca ostatnią
  kopię. Działają więc: lista projektów, projekt, lista drzew, karta drzewa, prace, mapa (punkty), katalog symboli,
  przywrócenie sesji (`/api/users/me`). Nie działają: zdjęcia (pliki), podkład mapy (kafelki OSM), dokumenty PDF,
  eksporty.
- **Kolejka zmian (V1):** dodanie drzewa, edycja danych drzewa, ustawienie / usunięcie lokalizacji (mapa, ręcznie,
  GPS), dodanie i edycja pracy. Zmiana od razu aktualizuje kopie (lista, karta, prace), więc ekran pokazuje nowy stan.
  Drzewa i prace utworzone offline mają **tymczasowe ujemne id** (adres karty `/trees/-123…`).
- **Synchronizacja:** FIFO, pojedynczo, istniejącymi endpointami (`POST /trees`, `PATCH /trees/{id}`,
  `PUT/DELETE .../location`, `POST/PATCH .../works`). Odpowiedź `POST` daje mapowanie tymczasowego id; kolejne
  zmiany dostają id z serwera. Uruchamia się: po powrocie sieci (zdarzenie `online` i każda udana odpowiedź
  serwera), co 30 s przy niepustej kolejce, przyciskiem „Synchronizuj teraz”.
- **Błędy:** brak sieci → synchronizacja czeka (kolejka bez zmian). Błąd serwera (walidacja, 404, 5xx) →
  synchronizacja staje na tej zmianie, pasek pokazuje opis zmiany i komunikat z serwera: **„Spróbuj ponownie”**
  albo **„Pomiń tę zmianę”** (pominięcie utworzenia drzewa pomija też zmiany tego drzewa).
- **Konflikty:** „ostatni zapis wygrywa” — `PATCH` wysyła komplet pól z urządzenia (jak formularz online).
- **Konto:** zmiany mają e-mail konta; wysyła je tylko to samo konto. Wylogowanie kasuje kopie odczytów
  (dane klientów na współdzielonym tablecie), niewysłane zmiany zostają do ponownego zalogowania tego konta.
- **Powłoka aplikacji:** service worker (`public/sw.js`) zapisuje `index.html` i pliki `/assets/` — aplikacja
  otwiera się po przeładowaniu bez sieci. Nigdy nie zapisuje `/api` ani kafelków mapy. Tylko w zbudowanej
  aplikacji (nie w `npm run dev`) i w bezpiecznym kontekście (HTTPS / localhost). To nie jest pełne PWA
  (bez manifestu, instalacji na ekranie głównym, powiadomień).

## UI

- Pasek w panelu: „Brak połączenia - pracujesz offline… (czeka: N zmian)”, „Na tym urządzeniu czeka na wysłanie:
  N zmian” + „Synchronizuj teraz”, „Synchronizowanie zmian…”, „Zsynchronizowano: N zmian”, błąd z „Spróbuj
  ponownie” / „Pomiń tę zmianę”.
- Lista drzew i prace: oznaczenie „Czeka na synchronizację”. Karta drzewa: informacja o zmianach na urządzeniu;
  dla drzewa utworzonego offline sekcje symbolu i zdjęć są ukryte („można dodać po synchronizacji”).
- Formularz drzewa: „Zmiany zapisano na tym urządzeniu - zostaną wysłane po połączeniu z internetem.”
- Po synchronizacji ekrany pobierają dane ponownie, a adres karty z tymczasowym id przekierowuje na id z serwera.

## Testy

- Frontend (`offlineQueue.test.ts`, magazyn w pamięci + fałszywy serwer): tymczasowe id i kopie, edycja /
  lokalizacja / praca offline, FIFO z podmianą id, przerwanie przy braku sieci i wznowienie, błąd walidacji
  (kolejka zachowana) i pominięcie ze zmianami zależnymi, tylko zmiany zalogowanego konta, opisy po polsku.
- Backend: sekwencja wysyłana przez synchronizację (utworzenie → lokalizacja → edycja → praca) działa na
  istniejących endpointach (`TreeLocationGnssApiIntegrationTest`).
- E2E (headless Chrome, emulacja braku sieci, PostgreSQL): przeładowanie strony bez sieci (service worker, sesja
  i lista z kopii), dodanie drzewa, edycja, GPS i praca offline, edycja istniejącego drzewa, oznaczenia, serwer
  bez zmian do czasu połączenia, automatyczna synchronizacja (5 zmian), przekierowanie z tymczasowego id,
  błąd synchronizacji z „Spróbuj ponownie”, ekran 390 px.

## Ograniczenia V1

- Zdjęcia, adnotacje, symbole, usuwanie prac, projekty, dokumenty, import/eksport — tylko z połączeniem.
- Brak nakładania niewysłanych zmian na świeże dane z serwera: gdy połączenie wróci, a zmiana czeka (np. po
  błędzie), ekran może pokazać stan serwera do czasu synchronizacji (pasek informuje o czekających zmianach).
- Brak kluczy idempotencji: jeśli żądanie dotarło do serwera, a odpowiedź zginęła (zerwane połączenie), synchronizacja
  może utworzyć duplikat drzewa / pracy.
- Podsumowanie projektu i mapa nie przeliczają się z kolejki offline (pokazują ostatnią kopię).
- Kopie odczytów nie mają limitu wieku ani rozmiaru.
- **Nie sprawdzono na prawdziwym tablecie w terenie** (słaby zasięg, przełączanie sieci, uśpienie urządzenia).

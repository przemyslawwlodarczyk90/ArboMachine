# Etap 30 — powiadomienia, kalendarz, finalizacja MVP

**Status:** V1 zaimplementowane; MVP gotowe do pokazania arborystom. Pytania: `docs/DO_DOPRECYZOWANIA.md`, sekcje
„Powiadomienia i Web Push”, „Kalendarz ICS”, „Wydanie MVP”. Część danych etapu (monitoring, grupy drzew, GIS):
`etap-29-monitoring-i-gis.md`.

## Powiadomienia

| Hipoteza | Realizacja V1 |
|---|---|
| Arborysta chce wiedzieć o drzewach w złym stanie | Kontrola z oceną „Zły” → powiadomienie (raz na kontrolę i datę) |
| Arborysta zapomina o terminach | Codziennie 7:00 (czas polski) jedno zbiorcze powiadomienie o pracach do wykonania z terminem na jutro |
| Powiadomienia mają dotrzeć na telefon | Web Push (bez aplikacji natywnej), włączany osobno na każdym urządzeniu: zakładka „Integracje” albo „Mój profil” |
| Trzeba sprawdzić, czy działa | „Wyślij powiadomienie testowe” → liczba urządzeń, do których wysłano |

- Model: `UserNotification` (lista w aplikacji, unikalny klucz deduplikacji), `WebPushSubscription` (urządzenie),
  `WebPushServerKey` (klucze VAPID, gdy nie podano ich w konfiguracji).
- Moduł `notification` nie zna monitoringu ani prac: nasłuchuje zdarzenia `TreeMonitoringRecordSavedEvent`
  (po zatwierdzeniu transakcji) i czyta prace przez repozytorium w zadaniu przypomnień. Wysyłka w osobnej puli wątków.
- Web Push zaimplementowany na kryptografii JDK: szyfrowanie treści RFC 8291 (aes128gcm, ECDH P-256 + HKDF),
  podpis VAPID RFC 8292 (ES256). Test na wektorze z RFC 8291; E2E: prawdziwa usługa FCM przyjęła wiadomość.
- Bezpieczeństwo: adres subskrypcji pochodzi od klienta, więc wysyłka tylko HTTPS do hostów z listy
  `app.push.allowed-endpoint-hosts` (ochrona przed SSRF). Odpowiedź 404/410 usługi push usuwa subskrypcję.
  Subskrypcje i powiadomienia są kasowane razem z kontem (`UserAccountDataCleaner`).
- Ograniczenia: iPhone/iPad tylko z aplikacją dodaną do ekranu głównego (iOS 16.4+); wymagany HTTPS.

## Kalendarz (ICS)

`GET /api/projects/{p}/exports/calendar.ics` — prace do wykonania (Zalecenie, Zaplanowana) z terminem jako
wydarzenia całodniowe: tytuł „nazwa pracy – drzewo X”, w opisie status, priorytet, drzewo z gatunkiem, projekt,
opis pracy; `GEO` z lokalizacją drzewa (jeśli jest). Zalecenie = `TENTATIVE`, Zaplanowana = `CONFIRMED`.
Stały `UID` (id pracy) — ponowny import aktualizuje wydarzenia. Format RFC 5545 pisany ręcznie (escapowanie,
zawijanie linii do 75 bajtów). Przycisk w zakładkach „Integracje” i „Import / eksport”.

## Finalizacja MVP

- **Zakładka „Integracje”** w projekcie: kalendarz, powiadomienia na tym urządzeniu, ostatnie powiadomienia.
- **Gospodarka drzewostanem:** filtr „najbliższe 7 dni”.
- **Dokument PDF:** dane zlecenia, monitoring, tabela grup drzew (kolejność naturalna: „Grupa 2” przed „Grupa 10”).
- **Wydanie:** `scripts/build-release.ps1` / `.sh` (testy, build, JAR + SHA-256), `docs/WYDANIE.md`.
- **Pokaz:** `scripts/demo/seed-demo-project.mjs` — projekt przykładowy albo z prawdziwych plików (`--sheet`, `--drawing`).
- **Dokumentacja:** README, `docs/ARCHITEKTURA.md`, rejestr braków „stan po etapie 30”.

## Podsumowanie etapów 27–30

| Etap | Co dodano | Charakter |
|---|---|---|
| 27 | Waloryzacja: zapis oceny arborysty (kategoria V1 + opisy wartości), tryb mapy, PDF, eksport | bez wzorów i kwot |
| 28 | Gospodarka drzewostanem: status, priorytet, terminy prac, widok projektu, tryb mapy, CSV prac | rozszerzenie `TreeWork` |
| 29 | Monitoring stanu, grupy drzew (obszary), import/eksport SHP / GPKG / DXF, PL-2000/PL-1992, arkusze aplikacji terenowej, symbol iglastych, dane zlecenia | V1 |
| 30 | Powiadomienia + Web Push, kalendarz ICS, zakładka „Integracje”, skrypty wydania i pokazu, dokumentacja | V1 |

Weryfikacja: testy backendu i frontendu zielone; E2E w headless Chrome (część A: 31/31, część B na prawdziwych
plikach arborysty: 22/22); eksporty GIS sprawdzone w GDAL 3.12.4; PDF dla 938 drzew — 530 stron, 4,4 s.

## Następne kroki (poza MVP)

Rozmowa z arborystami według `docs/DO_DOPRECYZOWANIA.md`: karta oceny stanu (materiał w etapie 29), metoda
waloryzacji, słowniki gatunków i prac, typy grup drzew, ustawienia powiadomień, hosting produkcyjny.

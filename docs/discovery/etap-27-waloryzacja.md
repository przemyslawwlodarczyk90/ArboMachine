# Etap 27 — waloryzacja drzew V1

**Status:** V1 zaimplementowane jako **zapis oceny arborysty**. Aplikacja NICZEGO nie wylicza: brak wzorów,
punktów, wag, współczynników i kwot. Pytania i założenia: `docs/DO_DOPRECYZOWANIA.md`, sekcja „Waloryzacja”.

> Uwaga o wymaganiach: pierwsza wersja polecenia etapu 27 proponowała wycenę w PLN wzorem typu
> „obwód² × wysokość”. Kolejna wersja polecenia (obowiązująca) wprost tego zabrania — metoda waloryzacji
> i ewentualnej wyceny pieniężnej nie jest znana i nie może być zgadywana. Zrealizowano wersję obowiązującą.

## Co wiemy

- Waloryzacja jest osobnym elementem dokumentacji arborystycznej (osobny rodzaj opracowania i/lub część
  innych opracowań).
- Arborysta wskazał wartości: **wiekową, przyrodniczą, ekologiczną, kulturową, krajobrazową**.

## Hipotezy V1 (zaimplementowane, do weryfikacji)

| Hipoteza | Realizacja V1 |
|---|---|
| Waloryzacja to ocena ekspercka, odrębna od pomiarów | Osobna encja `TreeValuation` 1:1 z `Tree` (tabela `tree_valuation`), osobny moduł `domain.treevaluation` |
| Wartości składowe zapisuje się słowami | Pola tekstowe (do 5000 znaków) z dyktowaniem: wiekowa, przyrodnicza, ekologiczna, kulturowa, krajobrazowa + uzasadnienie i uwagi |
| Przyda się prosta ocena ogólna (mapa, filtr, dokument) | Opcjonalna kategoria V1: Niska / Średnia / Wysoka / Bardzo wysoka (`LOW`/`MEDIUM`/`HIGH`/`VERY_HIGH`), brak = „Nie oceniono”. **To nie jest skala branżowa.** Zawsze wybierana ręcznie |
| Ocena może powstawać w terenie i w biurze | Formularz na karcie drzewa, duże przyciski, dyktowanie, praca offline (kolejka z etapu 26) |
| Ocena ogólna przydaje się na mapie | Tryb mapy „Waloryzacja”: kolor kategorii (umowny), legenda z liczbą drzew; symbole prac w osobnym trybie |
| Ocena trafia do dokumentu i eksportu | PDF: kolumna w zestawieniu drzew, blok „Waloryzacja” w karcie drzewa, liczba drzew z waloryzacją; CSV/XLSX: wszystkie pola; GIS: tylko kategoria |

## Model i API

- `TreeValuation`: `overallCategory` (varchar, nullable), `ageValue`, `naturalValue`, `ecologicalValue`,
  `culturalValue`, `landscapeValue`, `justification`, `notes` (text, nullable), `createdAt`, `updatedAt`.
  Bez historii wersji.
- `GET /api/projects/{projectId}/trees/{treeId}/valuation` → 200; drzewo bez waloryzacji: `hasValuation: false`.
- `PUT …/valuation` → zapis całej oceny (utwórz albo zastąp, idempotentny); pusta ocena → 400.
- `DELETE …/valuation` → 204 (także gdy nie było waloryzacji).
- `GET /api/projects/{projectId}/valuations` → waloryzacje drzew projektu (lista drzew, mapa).
- Dostęp wyłącznie Project → Tree → Valuation (cudzy projekt / drzewo z innego projektu → 404).

## Do potwierdzenia z arborystą

- Rzeczywista metoda waloryzacji (opisowa, punktowa, klasowa, mieszana?).
- Stosowane skale — czy wartości składowe są tekstem, czy klasami (np. I–V)?
- Czy istnieje wynik końcowy / ocena ogólna? Jak się ją ustala?
- Czy używane są punkty i wagi?
- Czy potrzebna jest wycena pieniężna? Jaką metodą (np. metody odtworzeniowe, tablice, rozporządzenia o opłatach
  za usunięcie drzew)? Kto ją wykonuje?
- Czy wartości składowe zależą od rodzaju opracowania?
- Jak waloryzacja wygląda w finalnym dokumencie (tabela zbiorcza? opis przy drzewie? klasy na mapie?).
- Jakie kolory / oznaczenia klas na mapach stosuje arborysta?
- Czy potrzebna jest historia ocen (np. kolejne inwentaryzacje)?
- Czy ocena dotyczy pojedynczych drzew, czy także grup / szpalerów / zadrzewień?

## Testy

- Backend (`TreeValuationApiIntegrationTest`): brak waloryzacji, zapis z przycięciem tekstu, odczyt, zastąpienie,
  brak zmian = bez zmiany `updatedAt`, walidacja (kategoria, długość, pusta ocena), usuwanie idempotentne, lista
  projektu, IDOR (inny użytkownik GET/PUT/DELETE, drzewo z innego projektu w URL), nieistniejący projekt,
  brak logowania, eksport CSV i GeoJSON, dokument PDF; brak pól pieniężnych w odpowiedzi.
- Frontend (`valuationFormValues.test.ts`, `valuationFilter.test.ts`, `mapViewMode.test.ts`,
  `offlineQueue.test.ts`): formularz, request, niezapisane zmiany, walidacja, filtr listy, legenda mapy,
  waloryzacja offline z synchronizacją.
- E2E (headless Chrome, PostgreSQL): opis w `docs/DO_DOPRECYZOWANIA.md` / raport etapu.

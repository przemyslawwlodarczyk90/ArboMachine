# Etap 28 — gospodarka drzewostanem V1

**Status:** V1 zaimplementowane jako **rozszerzenie istniejącego `TreeWork`** (zalecenia i prace z etapu 14) —
bez osobnego modelu `ManagementTask`. Pytania i założenia: `docs/DO_DOPRECYZOWANIA.md`, sekcja „Gospodarka drzewostanem”.

> Uwaga o wymaganiach: pierwsza wersja polecenia etapu 28 proponowała nową encję `ManagementTask`, endpointy
> `/management/tasks` i walidację `plannedDate >= dziś`. Obowiązująca wersja polecenia wprost tego zabrania
> (bez równoległego modelu obok `TreeWork`, terminy w przeszłości dozwolone — dokumentacja może opisywać projekty
> historyczne). Zrealizowano wersję obowiązującą.

## Co wiemy

- Gospodarka drzewostanem jest jednym z podstawowych typów pracy arborysty.
- Dotyczy planowania prac przy drzewach („co należy zrobić z drzewami w projekcie?”).
- Dane powinny być widoczne na mapach i w dokumentacji.
- Arborysta już zapisuje zalecenia i prace przy drzewie (`TreeWork`, etap 14) i oznacza je symbolami (etap 19).

## Hipotezy V1 (zaimplementowane, do weryfikacji)

| Hipoteza | Realizacja V1 |
|---|---|
| Praca ma cykl życia | `status`: Zalecenie / Zaplanowana / Wykonana / Anulowana (`RECOMMENDED`/`PLANNED`/`COMPLETED`/`CANCELLED`). Domyślnie Zalecenie. **Dowolne przejścia** — bez maszyny stanów |
| Prace mają różną pilność | `priority` opcjonalny: Niski / Normalny / Wysoki / Pilny (`LOW`/`NORMAL`/`HIGH`/`URGENT`). **Priorytet organizacyjny, nie skala branżowa**; nigdy wyliczany (np. z waloryzacji) |
| Prace mają termin | `plannedDate` opcjonalny, **może być w przeszłości**; „po terminie” to tylko informacja w UI |
| Wykonanie się odnotowuje | `completedDate` opcjonalny; „Oznacz jako wykonaną” ustawia status i dzisiejszą datę (jeśli brak) |
| Uzasadnienie pracy | Istniejące `description` (UI: „Opis”, eksport: „Opis / uzasadnienie”) — bez osobnego pola `justification` |
| Potrzebny jest widok całego projektu | Zakładka „Gospodarka drzewostanem”: wszystkie prace projektu, liczniki, filtry status / priorytet / drzewo, tabela (szeroki ekran) lub karty (tablet/telefon) |
| Prace widać na mapie | Tryb mapy „Gospodarka drzewostanem”: kolor markera = stan prac drzewa, znaczki tylko prac do wykonania; symbole rodzaju pracy (etap 19) bez zmian |
| Prace trafiają do dokumentu i eksportu | PDF: sekcja „5. Gospodarka drzewostanem” (tabela) + status/priorytet/terminy w karcie drzewa + liczniki; XLSX: arkusz „Prace”; osobny CSV prac; GeoJSON/GeoPackage: atrybuty podsumowujące |

## Model i API

- `TreeWork` (tabela `tree_work`) — nowe kolumny, wszystkie nullowe: `status` (varchar 32, null = prace sprzed
  etapu 28, czytane jako `RECOMMENDED`), `priority` (varchar 32), `planned_date` (date), `completed_date` (date).
  Kolumny dodaje Hibernate (`ddl-auto=update`) — bez migracji danych.
- `POST /api/projects/{projectId}/trees/{treeId}/works` i `PATCH …/works/{workId}` przyjmują dodatkowo `status`,
  `priority`, `plannedDate`, `completedDate` (PATCH = komplet pól, jak dotąd). Nieznany status/priorytet → 400.
- `GET /api/projects/{projectId}/works` — prace wszystkich drzew projektu; opcjonalny filtr
  `?status=PLANNED&status=RECOMMENDED&priority=URGENT` (wiele wartości = dowolna z nich).
- `GET /api/projects/{projectId}/exports/works.csv` — CSV prac (jeden wiersz = jedna praca).
- Dostęp wyłącznie Project → Tree → TreeWork (cudzy projekt / praca z innego drzewa w URL → 404).
- Brak nowych endpointów `/management/...` — widok projektowy używa istniejącego `GET …/works`.

## Eksport i QGIS

- Jedno drzewo może mieć wiele prac, więc **szczegóły prac są w osobnych tabelach**: arkusz XLSX „Prace” i CSV
  prac (kolumny `WorkExchangeColumn`: ID drzewa, numer, gatunek, ID pracy, praca, opis, status, priorytet, termin,
  data wykonania, symbol). Status i priorytet jako kody, daty RRRR-MM-DD.
- Atrybuty drzewa (CSV/XLSX drzew, GeoJSON, GeoPackage): `openWorkCount` / `works_open` (liczba prac do
  wykonania) i `nextPlannedWorkDate` / `work_next` (najbliższy termin pracy do wykonania).
- GeoJSON: tablica `works` przy drzewie zawiera dodatkowo `status`, `priority`, `plannedDate`, `completedDate`.
- Osobna warstwa/tabela prac w GeoPackage — świadomie odłożona.
- Import plików ignoruje pola gospodarki (jak dotąd prace nie są importowane).

## Offline

- Ta sama kolejka domenowa co w etapie 26: `CREATE_TREE_WORK` i `UPDATE_TREE_WORK` niosą nowe pola (kopie lokalne
  karty drzewa i listy projektu aktualizowane od razu), nowa operacja `DELETE_TREE_WORK` (usuwanie pracy offline).
- Konflikty: jak w etapie 26 — ostatni zapis wygrywa.

## Do potwierdzenia z arborystą

- Rzeczywiste kategorie prac (słownik zamiast wolnego tekstu) i ich kody.
- Stosowane symbole prac na mapach i w tabelach.
- Czy potrzebny jest priorytet? Jaką skalę stosuje arborysta (np. terminy ustawowe, „natychmiast / do 1 roku”)?
- Terminy / horyzonty planowania (konkretna data, sezon, rok, okres poza sezonem lęgowym?).
- Statusy i przejścia — czy jest „odebrana”, „odrzucona”, „w trakcie”? Kto zmienia status?
- Sposób prezentacji w dokumentacji (program prac konserwatorskich? tabela? mapa prac?).
- Czy jedna praca może dotyczyć grupy drzew (szpaler, zadrzewienie)?
- Czy potrzebne są prace projektowe niezwiązane z pojedynczym drzewem (np. nasadzenia zastępcze)?
- Czy wykonanie pracy jest śledzone w tym samym systemie (kto wykonał, protokół, zdjęcia po wykonaniu)?
- Koszty, wykonawcy, protokoły wykonania, załączniki.
- Daty kontroli i prace cykliczne (np. kontrola co rok).
- Czy prace opóźnione mają być wyróżniane (V1: „po terminie” jako informacja)?
- Konflikty offline przy pracy wielu osób (V1: ostatni zapis wygrywa).

## Świadomie NIE zaimplementowano

Workflow urząd → wykonawca, zleceń dla firm, kosztorysów, faktur, harmonogramu / kalendarza, powiadomień,
automatycznego generowania prac lub priorytetów z waloryzacji, maszyny stanów statusów, historii zmian (poza
`createdAt`/`updatedAt` i `completedDate`), importu prac.

## Testy

- Backend (`TreeWorkManagementApiIntegrationTest`): tworzenie ze statusem/priorytetem/terminem, domyślny status,
  praca sprzed etapu 28 (status null) czytana jako Zalecenie, dowolne przejścia i daty historyczne, walidacja
  statusu/priorytetu/daty, filtry listy projektu, usuwanie, wiele prac na drzewie, IDOR (inny użytkownik
  GET/PATCH/DELETE, praca przez URL innego drzewa i innego projektu, lista projektu), CSV prac, arkusz „Prace”,
  GeoJSON, dokument PDF. Dotychczasowe testy `TreeWork` bez zmian.
- Frontend (`workManagement.test.ts`, `treeWorkFormValues.test.ts`, `treeMapPoints.test.ts`, `mapViewMode.test.ts`,
  `offlineQueue.test.ts`): etykiety, filtr, kolejność, liczniki, stan drzewa na mapie i legenda, formularz i
  „Oznacz jako wykonaną”, praca offline z nowymi polami i usuwaniem + synchronizacja.
- Brak testów renderowania komponentów React (dług techniczny projektu).

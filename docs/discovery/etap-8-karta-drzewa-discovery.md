# Etap 8 — discovery pól karty drzewa

> **Status: DISCOVERY NIEWYKONANE.** Checkpoint z arborystą jeszcze się nie odbył.
> Do czasu jego przeprowadzenia **żadne nowe pole nie jest implementowane**.
> Ten dokument jest narzędziem do przeprowadzenia rozmowy i zapisania jej wyników,
> a nie listą wymagań.

Model `Tree` po etapie 7 zawiera wyłącznie: `inventoryNumber` (wymagany), `speciesLatin` (opcjonalny), `notes` (opcjonalny),
`createdAt`, `updatedAt`. Karta drzewa ma tryb podglądu i edycji (Save / Cancel).

Proces (KAIZEN): **discovery → jedno potwierdzone pole → DB → backend → API → frontend → test → arborysta sprawdza → następne pole.**

---

## 1. Scenariusz checkpointu z arborystą (8.1–8.2)

**Przygotowanie:** działająca aplikacja na tablecie; konto arborysty; jeden pusty projekt.

1. Pokaż cały pion: `Project → Trees → Add Tree → karta drzewa → Edit`.
2. Poproś o **jeden realny przykład drzewa** (najlepiej z ostatniego zlecenia, z notatkami / zdjęciem karty papierowej).
3. Nie pytaj „jakie pola powinny być w aplikacji?”. Przejdź proces terenowy:
   - „Stoimy teraz przy drzewie. Co robisz jako pierwsze?”
   - „Co wpisujesz?”
   - „Co mierzysz? Czym?”
   - „Co robisz później?”
   - „Które dane muszą być zawsze?”
   - „Które tylko czasami? Od czego to zależy?”
   - „Które dyktujesz?”
   - „Które wybierasz z listy? Z jakiej?”
4. Zapisz **dokładną kolejność** czynności przy drzewie (sekcja 2), a nie kolejność kolumn.

## 2. Kolejność terenowa (wypełnić po rozmowie)

| # | Czynność / dana (słowami arborysty) | Uwagi |
|---|-------------------------------------|-------|
| 1 | _do uzupełnienia_ | |
| 2 | | |
| 3 | | |

## 3. Karta metadanych pola (8.3) — kopiować dla każdego kandydata

```
POLE: ______________________
A. Nazwa biznesowa:
B. Co oznacza:
C. required / optional / conditional (jeśli conditional — od czego zależy):
D. Typ: text | integer | decimal | boolean | enum | multi-select | date | calculated
E. Jednostka (tylko jeśli dotyczy):
F. Zakres wartości:
G. Wpisywane ręcznie? (tak/nie)
H. Pochodzi z urządzenia? (jakiego)
I. Można dyktować głosem? (tak → VOICE CANDIDATE)
J. Gdzie widoczne: tabela / mapa / raport / eksport GIS
K. Może mieć wiele wartości?
L. Zmienia się w czasie? (historia pomiarów?)
M. Obserwacja czy pomiar?
Czy można zapisać Tree bez tej wartości? (tak → kolumna nullable)
Status: CONFIRMED | NEEDS CLARIFICATION | FUTURE
```

## 4. Checkpointy specjalne

### 4.1 Gatunek (8.5)

1. Czy arborysta wpisuje nazwę łacińską?
2. Czy potrzebuje również polskiej?
3. Czy wybiera gatunek ze słownika?
4. Czy wpisuje ręcznie?
5. Czy potrzebny autocomplete?
6. Czy system ma automatycznie wiązać nazwę polską ↔ łacińską?
7. Co z: samym rodzajem (genus only), kultywarem, mieszańcem, gatunkiem nieznanym?
8. Czy gatunek może zostać uzupełniony później?

Decyzja do podjęcia **po** odpowiedziach: czy `speciesLatin: String` wystarcza, czy później potrzebny słownik / encja `Species`.
Do tego czasu bez refaktoru.

### 4.2 Numer drzewa (8.6)

1. Czy numer jest nadawany ręcznie?
2. Czy ArboMap nadaje go automatycznie?
3. Czy numeracja startuje od 1?
4. Czy jest osobna dla projektu?
5. Czy dopuszczalne są numery typu A1, A2?
6. Czy numer może się zmienić?
7. Czy dwa drzewa mogą mieć ten sam numer?
8. Co przy imporcie istniejącej inwentaryzacji?
9. Czy istnieje numer terenowy vs numer bazodanowy?
10. Czy numer widnieje na mapie?

Dopiero potem: unikalność, auto-numeracja, podpowiedź następnego numeru. Obecnie `inventoryNumber` to wolny tekst bez unikalności.

### 4.3 Pomiary (8.7) i wielopniowość (8.8)

Dla każdej mierzonej wielkości ustalić:

- co dokładnie jest mierzone,
- w jakiej jednostce,
- w jaki sposób / jakim narzędziem,
- czy wartość jest dokładna czy szacowana,
- czy może istnieć kilka pomiarów (w czasie lub równocześnie),
- na jakiej wysokości wykonywany jest pomiar,
- czy korona ma jeden wymiar czy kilka osi,
- jak obsługiwane są drzewa wielopniowe.

**Nie projektujemy** kolumn `trunk1Circumference`, `circumference2` itp. Jeżeli wielopniowość jest realna,
prawdopodobnie potrzebna będzie osobna encja / value object (`TreeTrunk` / `TrunkMeasurement`). Decyzja po discovery.

### 4.4 Stan / zdrowie / ryzyko (8.9)

Nie tworzymy enumów typu `GOOD/AVERAGE/BAD` ani `LOW/MEDIUM/HIGH`. Ustalić:

- czy arborysta używa opisów tekstowych, VTA, własnych kategorii, checklist,
- czy formularz zależy od rodzaju zlecenia,
- czy lista wartości jest zamknięta i stabilna (warunek użycia enuma w Javie, 8.14).

## 5. Klasyfikacja kandydatów (stan na dziś — bez rozmowy z arborystą)

Wszystkie poniższe pozycje to **hipotezy do omówienia**, nie wymagania. Żadna nie ma statusu CONFIRMED.

| Pole (kandydat) | Status | Typ | Jednostka | Wymagane | Zaimplementowane |
|-----------------|--------|-----|-----------|----------|------------------|
| inventoryNumber | istniejące (etap 5) — reguły numeracji: NEEDS CLARIFICATION | text | — | tak | tak |
| speciesLatin | istniejące (etap 5) — słownik / nazwa polska: NEEDS CLARIFICATION | text | — | nie | tak |
| notes | istniejące (etap 5) | text | — | nie | tak |
| nazwa polska gatunku | NEEDS CLARIFICATION | ? | — | ? | nie |
| obwód / pierśnica pnia | NEEDS CLARIFICATION | ? (liczbowy?) | ? | ? | nie |
| wysokość drzewa | NEEDS CLARIFICATION | ? (liczbowy?) | ? | ? | nie |
| wymiar korony | NEEDS CLARIFICATION | ? | ? | ? | nie |
| pomiary wielu pni | NEEDS CLARIFICATION | ? (osobna encja?) | ? | ? | nie |
| stan / zdrowotność | NEEDS CLARIFICATION | ? | — | ? | nie |
| VTA, poziom ryzyka | FUTURE | — | — | — | nie |
| wycena, zalecenia prac, gospodarka drzewostanem | FUTURE | — | — | — | nie |
| geometria, GNSS, mapa, PostGIS, QGIS | FUTURE | — | — | — | nie |
| zdjęcia | FUTURE | — | — | — | nie |
| tryb offline | FUTURE | — | — | — | nie |

## 6. Pion implementacji jednego pola (8.10) i DoD pola (8.20)

Każde pole CONFIRMED to osobny, mały pion (osobny commit):

1. potwierdzone wymaganie (karta z sekcji 3) → 2. typ DB → 3. zmiana schematu (mała, logiczna — nie „add_all_tree_fields”) →
4. `Tree` → 5. `CreateTreeRequest` (jeśli dotyczy) → 6. `UpdateTreeRequest` → 7. `TreeResponse` → 8. mapper →
9. walidacja backendu → 10. handler w `domain/tree/service/treefacade/` → 11. test backendu (także na PostgreSQL) →
12. typ `Tree` we frontendzie → 13. `NewTreeDialog` → 14. `TreeEditForm` → 15. `TreeDetailsView` → 16. walidacja frontendu →
17. test ręczny → 18. weryfikacja przez arborystę.

Zasady: kolumna nullable, jeśli drzewo można zapisać bez wartości (8.12); pomiary liczbowe jako typ liczbowy z jednoznaczną jednostką
w nazwie/modelu, nigdy `"123 cm"` w tekście (8.13); enum tylko dla list zamkniętych, stabilnych i potwierdzonych (8.14);
formularz w kolejności pracy arborysty, nie kolumn DB (8.15).

DoD pojedynczego pola:

- [ ] znaczenie potwierdzone
- [ ] typ potwierdzony
- [ ] jednostka potwierdzona (jeśli dotyczy)
- [ ] required/optional ustalone
- [ ] istnieje zmiana schematu DB
- [ ] backend przechowuje pole
- [ ] Create obsługuje pole (jeśli powinien)
- [ ] Update obsługuje pole
- [ ] Response zwraca pole
- [ ] walidacja działa
- [ ] testy backendu działają
- [ ] typ frontendu posiada pole
- [ ] formularz pokazuje pole
- [ ] edycja działa
- [ ] podgląd działa
- [ ] odświeżenie zachowuje wartość
- [ ] realny przykład sprawdzony

## 7. Próba na realnym przykładzie (8.19) — notatki

Po 3–5 potwierdzonych polach: STOP i „dodajmy tym formularzem prawdziwe drzewo”. Obserwować:

| Obserwacja | Notatka |
|------------|---------|
| Gdzie się zatrzymuje | |
| Czego szuka | |
| Co wpisuje w złej kolejności | |
| Które pole omija | |
| Czego brakuje | |
| Co powinno być wyborem zamiast tekstu | |
| Co chciałby podyktować (VOICE CANDIDATE) | |

Pola oznaczone VOICE CANDIDATE nie dostają jeszcze Speech API (osobny PoC, 8.17). Offline (IndexedDB, kolejka, service worker)
poza zakresem (8.18).

## 8. Stan przygotowania karty drzewa na teren (8.16)

Sprawdzone automatycznie w headless Chrome (emulacja dotyku) dla karty drzewa w trybie podglądu i edycji:

| Urządzenie | Rozdzielczość | Poziomy scroll | Najmniejszy element dotykowy |
|------------|---------------|----------------|------------------------------|
| telefon | 390×844 | brak | 42,5 px |
| tablet pionowo | 768×1024 | brak | 42,5 px |
| tablet poziomo | 1024×768 | brak | 42,5 px |

Na wąskich ekranach lista `dt/dd` przechodzi w jedną kolumnę, a przyciski Save/Cancel zawijają się w wierszu akcji.

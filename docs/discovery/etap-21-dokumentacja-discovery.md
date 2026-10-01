# Etap 21 — dokumentacja

**Status:** discovery + przygotowanie; pierwszy generator zrealizowany w etapie 22 („Dokument projektu”, PDF).
**Zasada:** brak pełnej wiedzy domenowej nie blokuje implementacji. Decyzje odwracalne zamykamy w szablonie,
małym interfejsie albo assemblerze, a pytania zapisujemy tutaj i w `docs/DO_DOPRECYZOWANIA.md`.

## Cel

Sprawdzić, czy ArboMachine potrafi zebrać istniejące dane projektu i wygenerować z nich sensowny, **prawdziwy**
(nie mock) dokument, który można pokazać arborystom — i na jego podstawie prowadzić dalsze discovery.
Celem nie jest zastąpienie całego procesu tworzenia dokumentacji arborystycznej.

## Co wiemy z dotychczasowego discovery

- Workflow arborysty: przygotowanie materiałów → praca terenowa (dane drzew, lokalizacje, zdjęcia, opisy,
  zalecenia) → mapy i tabele → dokument końcowy.
- Program prac konserwatorskich zawiera m.in.: informacje o obiekcie, zleceniodawcy i autorze, kwalifikacje,
  cel opracowania, opis, techniki wykonania, tabelę drzew, opisy i zalecenia, mapy, fotografie z opisami.
- Inne znane rodzaje opracowań: inwentaryzacja drzewostanu, waloryzacja drzew, gospodarka drzewostanem,
  program ochrony drzew na placu budowy.
- **Nie znamy** dokładnego wzoru żadnego z tych dokumentów ani różnic między nimi.

## Jak obecnie arborysta tworzy dokument

Na podstawie dotychczasowych materiałów (konspekty): dane z terenu trafiają do arkuszy (XLS), szkice i mapy
powstają w ZWCAD / ArboMap / QGIS, a dokument końcowy jest składany ręcznie w edytorze tekstu (Word).
Szczegóły — jakie sekcje są stałe, co jest pisane ręcznie, ile zdjęć trafia do dokumentu — wymagają rozmowy
z arborystą na działającym przykładzie (pytania niżej).

## Dane dostępne obecnie w ArboMachine

### A. AVAILABLE (używane w dokumencie V1)

| Obszar | Dane |
|---|---|
| Project | nazwa, opis (wolny tekst), data utworzenia, data ostatniej zmiany |
| Tree | numer inwentaryzacyjny, gatunek polski, gatunek łaciński, obwód pnia [cm], wysokość [m], średnica korony [m], notatki, stan drzewa (wolny tekst, etap 17), zagrożenia (wolny tekst, etap 17) |
| TreeLocation | szerokość / długość geograficzna WGS84 (opcjonalne) |
| TreeWork | nazwa pracy / zalecenia (wolny tekst), opis (opcjonalny) |
| TreePhoto | plik zdjęcia (JPEG/PNG/WebP), nazwa pliku, data dodania |
| TreePhotoAnnotation | spłaszczony PNG zdjęcia z adnotacją (etap 20) — stabilny kontrakt „wersja do dokumentacji” |
| Podsumowanie | liczby: drzewa, drzewa z lokalizacją, zdjęcia, prace (liczone z tych samych danych) |

### B. MISSING BUT NON-BLOCKING (w V1 jawny placeholder albo pominięcie)

| Brak | Zachowanie V1 |
|---|---|
| Zleceniodawca | „Dane do uzupełnienia” (placeholder w szablonie) |
| Autor opracowania | „Dane do uzupełnienia” |
| Obiekt / lokalizacja obiektu (adres, działka) | „Dane do uzupełnienia” |
| Rodzaj opracowania | „Dokument projektu — wersja MVP” |
| Podpisy zdjęć | podpis techniczny: „Fot. N. Drzewo nr X — nazwa pliku” (+ „z adnotacją”) |
| Obraz mapy | tekst „Mapa projektu dostępna jest w aplikacji ArboMachine.” + współrzędne w kartach drzew |
| Miniatury zdjęć | generator sam zmniejsza zdjęcia (dłuższy bok 1600 px, JPEG) |

### C. MISSING AND POTENTIALLY IMPORTANT LATER

- Kwalifikacje i uprawnienia autora, dane firmy, logo / branding.
- Cel i podstawa opracowania, opis techniki wykonania prac.
- Numer i wersja dokumentu, status roboczy/ostateczny, historia wygenerowanych dokumentów.
- Podpisy, pieczęcie, podpis elektroniczny.
- Strukturalna ocena stanu (VTA, ryzyko), waloryzacja, priorytety i terminy prac, słownik prac.
- Symbole mapowe w dokumencie i legenda do druku.
- Załączniki (mapy, tabele w osobnych plikach).

## Dane, których NIE dodajemy bez potwierdzenia

Nie tworzymy encji ani pól: `Client`, `Author`, `Qualification`, `DocumentPurpose`, adres obiektu, numer działki,
typ opracowania, numer dokumentu. Każde z nich wymaga potwierdzenia, czy należy do projektu, do konta użytkownika
(np. dane autora), czy do konkretnego dokumentu — błędna decyzja oznaczałaby migrację danych. Do tego czasu
placeholdery w szablonie są tańsze i całkowicie odwracalne.

## Pierwszy dokument MVP

„Dokument projektu” (nazwa neutralna — nie „program prac konserwatorskich”, bo nie mamy potwierdzonego wzoru):

1. Strona tytułowa: ArboMachine, „Dokument projektu”, nazwa projektu, rodzaj opracowania, placeholdery
   zleceniodawcy i autora, data wygenerowania, informacja „dokument roboczy”.
2. Informacje o projekcie: nazwa, opis, placeholder obiektu, daty.
3. Podsumowanie: liczba drzew, drzew z lokalizacją, zdjęć, prac.
4. Mapa: tekst zastępczy (snapshot mapy odłożony — patrz decyzje).
5. Zestawienie drzew: numer, gatunek PL, gatunek LAT, obwód, wysokość, średnica korony („—” dla braków).
6. Karty drzew: gatunek, pomiary, lokalizacja, stan, zagrożenia, notatki (tylko niepuste), zalecenia i prace,
   zdjęcia (wersja z adnotacją, jeśli istnieje) z numeracją „Fot. N”.

## Proponowana architektura generatora

```
GET /api/projects/{projectId}/documents/project-report
        |
ProjectDocumentController  ->  ProjectDocumentFacade  ->  GenerateProjectReportHandler
        |
FindProjectOwnedByCurrentUserHandler        (własność: cudzy / nieistniejący projekt -> 404)
        |
ProjectDocumentDataAssembler                (4 zapytania na projekt, bez N+1, transakcja tylko do odczytu)
        |
ProjectDocumentData (+ Tree/Location/Work/PhotoData)   <- jawny model, bez encji JPA
        |
DocumentGenerator  (interfejs)  <-  ProjectReportPdfGenerator (V1)
        |
templates/documents/project-report.html  (Thymeleaf)  ->  jsoup (DOM)  ->  openhtmltopdf (PDFBox)  ->  PDF
```

- **Format:** PDF generowany na żądanie, bez zapisu pliku i bez encji dokumentu.
- **Pipeline HTML → PDF:** Thymeleaf był już w projekcie (maile), więc szablon to zwykły HTML/CSS łatwy do zmiany
  po konsultacji. openhtmltopdf (utrzymywany fork `io.github.openhtmltopdf`, PDFBox 3) — czysta Java, bez
  przeglądarki ani usług zewnętrznych; obsługuje A4, `@page`, numery stron, powtarzanie nagłówków tabel
  i `page-break-inside`. Alternatywy odrzucone: rysowanie PDF w kodzie (trudne do zmiany), headless Chrome
  (ciężka infrastruktura), DOCX (brak potwierdzenia potrzeby; patrz pytania).
- **Czcionka:** DejaVu Sans osadzona w aplikacji (`resources/fonts/documents`, licencja w katalogu) — polskie znaki
  niezależnie od systemu serwera.
- **Zdjęcia:** czytane leniwie dopiero przy renderowaniu (własny protokół `report-image://N`), zmniejszane do 1600 px
  i kodowane jako JPEG, każde raz; uszkodzony plik → zdjęcie pominięte, podpis zostaje, dokument powstaje.
- **Wymiana generatora:** nowy rodzaj dokumentu = nowa implementacja `DocumentGenerator` i nowy szablon;
  assembler i model danych zostają.

## Placeholders zastosowane w V1

| Miejsce | Tekst |
|---|---|
| Rodzaj opracowania | Dokument projektu — wersja MVP |
| Zleceniodawca | Dane do uzupełnienia |
| Autor opracowania | Dane do uzupełnienia |
| Obiekt / lokalizacja | Dane do uzupełnienia |
| Mapa | Mapa projektu dostępna jest w aplikacji ArboMachine. |
| Brak opisu projektu | Nie podano |
| Brak gatunku | Gatunek nie podany |
| Brak wartości w tabeli / pomiarze | — |
| Brak lokalizacji | Nie podano |
| Brak prac / zdjęć | Brak zapisanych zaleceń. / Brak zdjęć. |
| Zdjęcie WebP bez adnotacji | Zdjęcia w tym formacie nie można jeszcze umieścić w dokumencie PDF — dostępne w aplikacji. |

Placeholdery są w szablonie, wyróżnione kursywą i kolorem — nigdy fikcyjne nazwiska, firmy ani adresy.

## Decyzje odwracalne

- PDF na żądanie, bez historii, wersji ani statusu dokumentu.
- Jeden neutralny szablon dla wszystkich projektów.
- Placeholdery zamiast nowych pól domenowych.
- Mapa jako tekst zamiast obrazu (snapshot OpenLayers wymagałby renderowania mapy po stronie serwera albo
  przesyłania obrazu z przeglądarki — osobny krok).
- Zdjęcia pod kartą każdego drzewa; numeracja „Fot. N” w całym dokumencie.
- Kolejność drzew = kolejność dodania (jak w aplikacji).
- Daty w strefie Europe/Warsaw; liczby z polskim przecinkiem.
- Zdjęcia zmniejszane do 1600 px, JPEG 85%, bez obrotu wg EXIF.
- Nazwa pliku `arbomachine-projekt-{id}.pdf` (bez nazwy projektu — nie trzeba jej sanityzować).

## Pytania do arborysty

Do zadania **po pokazaniu działającego generatora**:

1. Które sekcje dokumentu są zawsze obowiązkowe?
2. Które sekcje zależą od rodzaju opracowania (program prac konserwatorskich, inwentaryzacja, waloryzacja,
   gospodarka drzewostanem, ochrona drzew na budowie)?
3. Czy dokument powinien być edytowalny przed eksportem? Jakie fragmenty pisze Pan/Pani zawsze ręcznie?
4. Czy potrzebny jest DOCX (do dalszej edycji w Wordzie), czy wystarczy PDF?
5. Jak powinny wyglądać tabele drzew — jakie kolumny, kolejność, jednostki, sortowanie?
6. Czy fotografie powinny być przy każdym drzewie, w osobnym rozdziale, czy w załączniku?
7. Ile zdjęć typowo trafia do raportu? Wszystkie, czy wybrane? Jaki rozmiar zdjęcia na stronie?
8. Czy wszystkie zalecenia mają być prezentowane? W jakiej formie (tabela zbiorcza prac)?
9. Jak wygląda numeracja zdjęć? Czy zdjęcia mają opisy pisane ręcznie?
10. Jak wygląda numeracja tabel i rozdziałów?
11. Czy mapa jest częścią dokumentu, czy osobnym załącznikiem? W jakiej skali, z jakim podkładem i legendą?
12. Jakie dane autora są wymagane (imię, nazwisko, uprawnienia, numer certyfikatu, firma)?
13. Jakie dane zleceniodawcy są wymagane?
14. Czy wymagane są podpisy / pieczęcie / podpis elektroniczny?
15. Czy istnieją różne wzory dla różnych klientów / urzędów?
16. Czy dokument ma numer, wersję, status (roboczy / ostateczny) i datę opracowania inną niż data wygenerowania?
17. Czy w dokumencie potrzebne są stan drzewa i zagrożenia w obecnej formie (wolny tekst)?
18. Czy potrzebne jest logo firmy / stopka firmowa?

Nie odpowiadamy za arborystę — do tego czasu obowiązują decyzje V1.

## Pomysły na dalsze iteracje

- Dane autora z profilu użytkownika (konto już ma imię i nazwisko) + pola firmy / uprawnień po potwierdzeniu.
- Pola dokumentu wypełniane przed generowaniem (zleceniodawca, obiekt, cel) — jeśli okaże się, że należą
  do dokumentu, a nie do projektu.
- Wybór zdjęć do dokumentu i własne podpisy zdjęć.
- Obraz mapy z symbolami i legendą (np. snapshot z przeglądarki przesyłany przy generowaniu).
- Tabela zbiorcza prac / zaleceń.
- Szablony per rodzaj opracowania (kolejne implementacje `DocumentGenerator`).
- DOCX, jeśli arborysta potwierdzi potrzebę ręcznej edycji.
- Historia wygenerowanych dokumentów (wersje, status, data przekazania) — dopiero przy realnej potrzebie.
- Generowanie w tle przy bardzo dużych projektach.

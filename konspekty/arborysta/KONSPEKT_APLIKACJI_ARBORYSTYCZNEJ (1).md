# KONSPEKT PROJEKTU

## Zintegrowane narzędzie do inwentaryzacji, oceny i dokumentacji drzewostanu

> **Status dokumentu:** roboczy konspekt produktu\
> **Cel:** zebranie tego, co już wiadomo o potrzebach odbiorcy, obecnym
> procesie pracy, ArboMap i QGIS, oraz wskazanie obszarów wymagających
> dalszego doprecyzowania.

Materiał powstał na podstawie: - pierwszej rozmowy z arborystą, -
odpowiedzi mailowych na pytania dotyczące procesu pracy, - materiałów i
informacji o ArboMap, - przykładów rzeczywistej dokumentacji
arborystycznej.

------------------------------------------------------------------------

## 1. Ogólna idea projektu

Celem projektu jest stworzenie narzędzia wspierającego pracę arborysty
**od otrzymania materiałów źródłowych i pracy terenowej aż do
przygotowania finalnej dokumentacji dla klienta**.

Pierwotna idea została podczas rozmowy określona jako stworzenie swego
rodzaju **„kombajnu"**, który ograniczyłby konieczność korzystania z
wielu niezależnych programów oraz ręcznego przenoszenia danych pomiędzy
nimi.

Obecnie poszczególne części procesu realizowane są m.in. przy
wykorzystaniu:

**ArboMap → QGIS / CAD → Excel → zdjęcia → dokument końcowy**

w zależności od rodzaju wykonywanego zlecenia.

ArboMap dobrze wspiera część terenową, ale nie rozwiązuje całego
procesu. Z kolei QGIS i CAD zapewniają duże możliwości obróbki
przestrzennej i graficznej, jednak wymagają dalszej pracy po zakończeniu
inwentaryzacji.

Z wypowiedzi arborysty wynika, że po zebraniu danych nadal pozostaje
znaczna część pracy wykonywanej ręcznie:

> „W biurze przetwarzam zebrane dane."

> „Tworzę dokument do moich potrzeb."

> „Obecnie dla moich potrzeb modyfikuję mapy o brakujące w arbo symbole
> prac, tabelki w zw cad."

> „W zw cad na fotografiach dorysowuję szkice cięć i umieszczam je w
> dokumencie."

Dlatego potencjalną główną wartością nowego rozwiązania nie jest
wyłącznie zastąpienie ArboMap, ale **połączenie zbierania danych, danych
przestrzennych, oceny arborystycznej i tworzenia dokumentacji w jeden
proces**.

------------------------------------------------------------------------

## 2. Problem, który próbujemy rozwiązać

Obecny proces jest rozproszony.

Informacja dotycząca jednego drzewa może pojawiać się kolejno:

**w terenie → w ArboMap → w XLS → na mapie → w CAD → na fotografiach → w
raporcie końcowym.**

Prowadzi to potencjalnie do:

-   wielokrotnego przetwarzania tych samych danych,
-   ręcznego przygotowywania dokumentów,
-   konieczności pracy w kilku programach,
-   problemów z kompatybilnością formatów,
-   ręcznego tworzenia map i oznaczeń,
-   ręcznego przypisywania lub obróbki materiałów fotograficznych,
-   trudności z przekazywaniem danych do systemów innych podmiotów.

Ten problem został wskazany już w pierwszej rozmowie:

> „Mój pomysł jest taki, żeby stworzyć kombajn, który to wszystko
> ogarnie."

oraz:

> „W tej chwili jest to wiele aplikacji niewspółgrających ze sobą."

To jest obecnie **najważniejsze założenie całego projektu**.

Nie chodzi o stworzenie kolejnego programu do stawiania punktów na
mapie.

Chodzi o stworzenie **spójnego przepływu danych przez cały proces
realizacji zlecenia arborystycznego**.

------------------------------------------------------------------------

## 3. Rzeczywisty przebieg zlecenia

Na podstawie odpowiedzi arborysty możemy już zarysować rzeczywisty
workflow.

### ETAP 1 -- otrzymanie zlecenia

Klient wskazuje konkretne drzewa lub cały drzewostan.

Przykładowym zleceniem może być:

> „Program prac konserwatorskich dla wskazanych drzew lub całego
> drzewostanu."

Arborysta otrzymuje lub sam pozyskuje informacje źródłowe.

Mogą to być:

-   mapy DWG,
-   mapy geodezyjne,
-   rastry PDF,
-   informacje dotyczące obiektu,
-   informacje o ochronie konserwatorskiej,
-   informacje o pomnikach przyrody,
-   inne dane administracyjne i przestrzenne.

Co istotne:

> „Po mojej stronie jest znalezienie źródłowych informacji."

Czyli potencjalnie przyszły system może nie tylko przyjmować gotowe
materiały klienta, ale również pomagać w **organizacji danych źródłowych
projektu**.

------------------------------------------------------------------------

## 4. Przygotowanie projektu

Przed rozpoczęciem pracy terenowej należy utworzyć projekt/zlecenie.

Logicznie taki projekt powinien posiadać m.in.:

-   nazwę,
-   klienta/zleceniodawcę,
-   lokalizację,
-   granice opracowania,
-   typ opracowania,
-   materiały źródłowe,
-   mapy,
-   osoby wykonujące,
-   informacje formalne,
-   konfigurację danych zbieranych dla drzewa.

Nie wiadomo jeszcze, ile z tych elementów powinno być obowiązkowych.

Istotne jest jednak, że **projekt nie może być ograniczony wyłącznie do
inwentaryzacji**.

------------------------------------------------------------------------

## 5. Rodzaje obsługiwanych opracowań

Z odpowiedzi otrzymaliśmy już kilka konkretnych rodzajów dokumentacji.

### Program prac konserwatorskich

To obecnie jeden z najczęstszych rodzajów zlecenia.

Arborysta napisał:

> „Najczęstszymi moimi zleceniami są programy prac konserwatorskich dla
> drzew."

Przykładowa dokumentacja pokazuje, że taki dokument może zawierać m.in.:

-   ogólny opis prac,
-   przewidywany efekt,
-   czynności przewidziane pracami,
-   metody i techniki wykonania,
-   tabelaryczne zestawienie drzew,
-   mapę,
-   fotografie,
-   propozycje cięć.

### Inwentaryzacja drzewostanu

Według arborysty:

> „inwentaryzacja jest dokumentem wyjściowym, informacyjnym"

Czyli stanowi podstawową warstwę danych, na której potencjalnie można
budować kolejne opracowania.

### Waloryzacja drzew

> „waloryzacja rozróżnia drzewa pod względem wieku, wartości
> przyrodniczej, kulturowej, krajobrazowej"

Jest to więc kolejna warstwa interpretacyjna nakładana na dane
inwentaryzacyjne.

### Gospodarka drzewostanem

> „gospodarka drzewostanem jest planem podstawowych prac przy drzewach."

Czyli następuje przejście:

**co istnieje → jaka jest wartość → co należy zrobić.**

### Program ochrony drzew na placu budowy

> „Zawierają one informacje (w tym w formie map) inwentaryzacyjne,
> zalecane strefy ochrony, m.in. wykluczenia z terenu budowy itp."

To bardzo ważne, ponieważ pokazuje, że system nie może przechowywać
wyłącznie **punktu drzewa**.

Potrzebne będą różne geometrie przestrzenne.

------------------------------------------------------------------------

## 6. Centralny model: PROJEKT

Na obecnym etapie najbardziej logiczna wydaje się architektura domenowa:

**PROJEKT**

→ materiały źródłowe\
→ mapa\
→ drzewa / inne obiekty\
→ pomiary\
→ oceny\
→ zdjęcia\
→ waloryzacja\
→ zalecenia\
→ geometrie/strefy\
→ dokumentacja\
→ eksporty

Typ projektu określałby natomiast, które elementy są potrzebne.

To ważniejsze niż tworzenie osobnego programu dla:

-   inwentaryzacji,
-   waloryzacji,
-   gospodarki,
-   programu konserwatorskiego,
-   ochrony drzew na budowie.

**Te dokumenty powinny korzystać z tego samego modelu danych.**

To jest na razie wniosek projektowy, a nie wymaganie potwierdzone przez
odbiorcę.

------------------------------------------------------------------------

## 7. Praca terenowa

To jeden z głównych modułów.

Obecnie arborysta wykorzystuje tablet z ArboMap.

Workflow wygląda mniej więcej tak:

**lokalizacja drzewa → ID → gatunek → wymiary → ocena → zagrożenia →
zalecenia → zdjęcia.**

Sam zainteresowany opisuje to tak:

> „Przy pomocy tabletu z aplikacją arbo opisuję drzewo: przyznaję numer
> id, określam gatunek (łacina), podaję podstawowe wymiary, oceniam stan
> zdrowotny oraz zagrożenia związane z drzewem, podaję zalecenia co do
> postępowania z drzewem."

Nowy program musi co najmniej zapewnić podobną wygodę.

------------------------------------------------------------------------

## 8. Lokalizacja drzewa

Istnieją obecnie dwa scenariusze.

### Drzewo istnieje na podkładzie

Arborysta porównuje jego rzeczywiste położenie z mapą geodezyjną.

### Drzewa nie ma na podkładzie

Należy nanieść je ręcznie albo przy wykorzystaniu GNSS.

> „Arbo ułatwia lokalizacje w dużych obszarach z brakiem punktów
> odniesienia. Aplikacja i transponder GNSS samodzielnie nanoszą na
> mapie punkt drzewa."

To oznacza potrzebę obsługi:

-   lokalizacji GNSS,
-   ręcznego wskazywania obiektu,
-   prawdopodobnie korekty lokalizacji,
-   pracy na różnych podkładach.

Szczegóły dotyczące urządzeń, RTK, dokładności i zachowania offline
nadal wymagają doprecyzowania.

------------------------------------------------------------------------

## 9. Mapa jako podstawowy element systemu

Mapa nie jest dodatkiem do programu.

Jest jednym z głównych interfejsów pracy.

Potrzebne są co najmniej:

-   podkłady geodezyjne,
-   rastry,
-   DWG/DXF lub ich konwersja,
-   dane GIS,
-   lokalizacje drzew,
-   symbole,
-   korony,
-   strefy ochronne,
-   oznaczenia prac,
-   legendy,
-   opisy.

ArboMap już potrafi pracować z DXF, WMS Geoportalu i rastrami oraz
eksportować m.in. DXF, PDF i Shapefile.

Nie ma więc sensu budować nowego rozwiązania tylko po to, żeby powtórzyć
te możliwości.

Należy ustalić, **czego brakuje pomiędzy ArboMap a QGIS/CAD**.

------------------------------------------------------------------------

## 10. Karta drzewa

Drzewo powinno być jednym z centralnych obiektów domenowych.

Na podstawie dotychczasowych materiałów karta może obejmować m.in.:

### Identyfikację

-   ID,
-   numer inwentaryzacyjny,
-   gatunek polski,
-   gatunek łaciński.

### Pomiary

-   obwód pnia,
-   wysokość,
-   średnicę korony,
-   inne parametry zależne od opracowania.

### Lokalizację

-   współrzędne,
-   pozycję na mapie,
-   potencjalnie dokładność pomiaru.

### Stan

-   stan zdrowotny,
-   stan korony,
-   statykę/stabilność,
-   uszkodzenia,
-   zagrożenia,
-   uwagi.

### Zalecenia

-   rodzaj zabiegu,
-   opis prac,
-   priorytet/termin -- jeżeli okaże się potrzebny.

### Multimedia

-   fotografie,
-   opisy fotografii,
-   potencjalnie nagrania głosowe.

Przykładowa tabela inwentaryzacyjna pokazuje m.in. numer, gatunek,
obwód, średnicę korony, wysokość, stan zdrowotny, stabilność, uwagi,
zalecenia i współrzędne.

Jednocześnie rzeczywisty program prac pokazuje, że opis może być bardzo
rozbudowany i zawierać informacje o warunkach wzrostu, pniu, koronie,
systemie korzeniowym, rozwidleniach, wcześniejszych badaniach i
istniejących zabezpieczeniach.

Dlatego **sztywna karta drzewa prawdopodobnie nie wystarczy**.

------------------------------------------------------------------------

## 11. Konfigurowalne formularze

ArboMap już posiada mechanizm dostosowywania pytań i odpowiedzi.

Nowy system prawdopodobnie również powinien umożliwiać tworzenie
formularzy zależnych od:

-   rodzaju projektu,
-   gminy,
-   klienta,
-   metody oceny,
-   rodzaju dokumentacji.

Przykładowo:

**Program prac konserwatorskich**

może wymagać innego formularza niż:

**waloryzacja**

a jeszcze innego:

**ochrona drzewa na placu budowy.**

Nie wiadomo jeszcze, jak daleko użytkownik powinien mieć możliwość
samodzielnego konfigurowania takich formularzy.

------------------------------------------------------------------------

## 12. Wprowadzanie danych głosem

To funkcja, którą zainteresowany wyraźnie ocenia pozytywnie.

> „Arbo tę część pracy mocno ułatwia przez zapisywanie w kolejnych
> polach tekstu przetworzonego z mowy."

Nie należy więc jej traktować jako ciekawostki.

W terenie arborysta może mieć zajęte ręce sprzętem pomiarowym, dlatego:

**mowa → tekst → konkretne pole formularza**

jest realnym usprawnieniem procesu.

Nowe rozwiązanie powinno co najmniej zachować ten poziom wygody.

------------------------------------------------------------------------

## 13. Dokumentacja fotograficzna

To kolejny bardzo istotny moduł.

Obecnie:

> „Wykonuję dokumentacje fotograficzne każdego drzewa."

ArboMap ma ważną zaletę:

> „fotografie z tabletu przypisywane są dla konkretnego drzewa."

Ale wskazano jednocześnie konkretną wadę:

> „brak tu możliwości współpracy z dobrym aparatem foto - szkoda"

To daje konkretne potencjalne wymaganie produktowe.

System powinien rozważać możliwość łatwego importowania fotografii
wykonanych aparatem i przypisywania ich do drzewa.

Przykładowo:

**Projekt → drzewo 006 → zdjęcia → korona / pień / rozwidlenie /
uszkodzenie / inne**

------------------------------------------------------------------------

## 14. Rysowanie na fotografiach

To jedna z ciekawszych możliwości nowego produktu.

Obecnie zainteresowany wykonuje dodatkowy krok:

> „W zw cad na fotografiach dorysowuję szkice cięć i umieszczam je w
> dokumencie."

Można więc rozważyć prosty edytor graficzny bezpośrednio w systemie:

**wybór fotografii → linia / obszar / strzałka / opis → zapis.**

Nie chodzi o tworzenie kolejnego CAD.

Chodzi o specjalizowane narzędzie typu:

**„zaznacz na tym zdjęciu, które części korony mają zostać usunięte".**

------------------------------------------------------------------------

## 15. Waloryzacja

Pierwotna rozmowa bardzo mocno podkreślała potrzebę połączenia:

**inwentaryzacji + waloryzacji + gospodarki drzewostanem.**

Waloryzacja powinna pozwalać określić wartość drzewa pod różnymi
względami.

Z maila mamy:

-   wiek,
-   wartość przyrodniczą,
-   wartość kulturową,
-   wartość krajobrazową.

Docelowo wynik powinien być powiązany również z mapą.

Pierwotna idea była taka, żeby projektant mógł od razu zobaczyć, że
konkretne drzewo jest szczególnie wartościowe i powinno zostać
uwzględnione przy projektowaniu inwestycji.

**Nie znamy jeszcze dokładnego sposobu wykonywania waloryzacji.**

To jest jedna z najważniejszych rzeczy pozostających do ustalenia.

------------------------------------------------------------------------

## 16. Gospodarka drzewostanem

Gospodarka powinna wykorzystywać dane z inwentaryzacji i oceny.

Na wysokim poziomie:

**INWENTARYZACJA**\
↓\
co mamy?

**WALORYZACJA / OCENA**\
↓\
jaką ma wartość / stan?

**GOSPODARKA**\
↓\
co z tym robimy?

Możliwe działania obejmują np.:

-   cięcia,
-   redukcje,
-   zabezpieczenia,
-   kontrolę,
-   dodatkowe badania,
-   pozostawienie bez ingerencji,
-   potencjalnie usunięcie.

W rzeczywistych dokumentach mogą być stosowane skróty/kody działań, co
sugeruje, że system może posiadać **bibliotekę typów prac**, zamiast
przechowywać wszystko jako dowolny tekst.

------------------------------------------------------------------------

## 17. Automatyzacja map

To może być jedna z głównych wartości aplikacji.

Arborysta napisał:

> „Następna częścią są mapy lokalizacji drzew z symbolami oznaczającymi
> rodzaj działań przewidzianych do wykonania dla danego drzewa."

oraz:

> „Obecnie dla moich potrzeb modyfikuję mapy o brakujące w arbo symbole
> prac, tabelki w zw cad."

Idealnie użytkownik powinien określić np.:

**Drzewo 006 → określone typy prac**

a program powinien wiedzieć:

-   jaki symbol pokazać,
-   jaki kolor zastosować,
-   jak utworzyć legendę,
-   jakie informacje pokazać na mapie,
-   jakie dane umieścić w raporcie.

To eliminuje wielokrotne ręczne kodowanie tej samej informacji.

------------------------------------------------------------------------

## 18. Automatyczne generowanie dokumentacji

Tutaj znajduje się potencjalnie **największy obszar automatyzacji**.

Dane raz wprowadzone w terenie powinny być ponownie wykorzystywane.

Przykładowo:

**DRZEWO 006**

ma:

-   lokalizację,
-   gatunek,
-   parametry,
-   opis,
-   ocenę,
-   zalecenia,
-   fotografie,
-   oznaczenia prac.

Na tej podstawie system może potencjalnie wygenerować:

**tabelę + mapę + kartę drzewa + podpisy zdjęć + legendę + część
raportu.**

Obecne dokumenty mają powtarzalną strukturę, więc przynajmniej część
składu wygląda na dobrego kandydata do automatyzacji.

Nie oznacza to, że cały raport może być automatyczny. Nie wiadomo
jeszcze, które części arborysta zawsze pisze indywidualnie.

------------------------------------------------------------------------

## 19. Szablony dokumentów

System prawdopodobnie powinien posiadać **szablony opracowań**.

Przykładowo:

`Nowy projekt`

→ Program prac konserwatorskich\
→ Inwentaryzacja\
→ Waloryzacja\
→ Gospodarka drzewostanem\
→ Ochrona drzew na budowie

Każdy szablon może definiować:

-   wymagane dane,
-   formularze,
-   rodzaje map,
-   legendę,
-   symbole,
-   tabelę,
-   strukturę raportu,
-   rodzaj eksportu.

To mogłoby być jednym z fundamentów całego produktu.

------------------------------------------------------------------------

## 20. QGIS

Na tym etapie **nie ma uzasadnienia dla próby napisania własnego
QGIS-a**.

QGIS jest rozbudowanym systemem GIS i odtwarzanie jego możliwości byłoby
ogromnym i niepotrzebnym przedsięwzięciem.

Nowe narzędzie powinno raczej:

**dobrze współpracować z QGIS.**

Czyli np.:

**nasz system → GeoPackage/SHP/inny GIS → QGIS**

oraz potencjalnie:

**QGIS → projekt → nasz system.**

Nadal trzeba dokładnie poznać workflow zainteresowanego w QGIS.

------------------------------------------------------------------------

## 21. CAD

Podobnie nie ma sensu pisać pełnego CAD.

Ale można usunąć konieczność korzystania z CAD do **prostych,
powtarzalnych czynności arborystycznych**.

Przykłady już znamy:

-   dodawanie brakujących symboli prac,
-   tworzenie prostych tabel,
-   oznaczanie elementów mapy,
-   nanoszenie zakresu cięć na fotografię.

To jest zasadnicza różnica:

**nie zastępujemy CAD → usuwamy potrzebę uruchamiania CAD do prostych
czynności.**

------------------------------------------------------------------------

## 22. Integracje i interoperacyjność

To było jednym z głównych tematów pierwszej rozmowy.

Program powinien być możliwie otwarty na różne formaty.

Już dziś można brać pod uwagę:

-   XLSX,
-   CSV,
-   PDF,
-   DXF,
-   DWG po odpowiedniej obsłudze/konwersji,
-   SHP,
-   GeoPackage,
-   rastry,
-   WMS.

Ale konkretna lista formatów musi wynikać z dalszych odpowiedzi.

Szczególnie ważne jest ustalenie, **co faktycznie wymagają gminy i
projektanci**.

------------------------------------------------------------------------

## 23. Jedno źródło danych

To jeden z najważniejszych wniosków technicznych.

Zamiast:

**ArboMap → XLS → CAD → Word → PDF**

należy dążyć do:

**CENTRALNY PROJEKT**\
↙ ↓ ↘\
teren \| GIS \| dokumentacja

Czyli np. zmiana zalecenia dla drzewa powinna automatycznie być
widoczna:

-   w karcie drzewa,
-   tabeli,
-   mapie,
-   legendzie,
-   raporcie.

Użytkownik nie powinien poprawiać tej samej informacji w czterech
miejscach.

------------------------------------------------------------------------

## 24. Potencjalne MVP

Jeszcze za wcześnie, żeby je zamknąć, ale na podstawie obecnych
informacji można już postawić hipotezę.

Pierwsza wersja mogłaby obejmować:

1.  Projekt/zlecenie.
2.  Import mapy/podkładu.
3.  Mapę terenową.
4.  Lokalizację drzewa GNSS.
5.  Kartę drzewa.
6.  Konfigurowalne formularze.
7.  Wprowadzanie głosowe.
8.  Zdjęcia przypisane do drzewa.
9.  Zalecenia i typy prac.
10. Automatyczne symbole na mapie.
11. Eksport danych.
12. Generator podstawowego raportu.

To jednak **nie jest jeszcze zatwierdzony zakres MVP**.

Dalsze odpowiedzi mogą go znacząco zmienić.

------------------------------------------------------------------------

## 25. Funkcje potencjalnie wyróżniające produkt

Na podstawie obecnych informacji widać kilka rzeczy, które mogą sprawić,
że nie będzie to po prostu kopia ArboMap.

### A. Jeden projekt od terenu do dokumentacji

Największa potencjalna różnica.

### B. Waloryzacja + gospodarka + inwentaryzacja

Pierwotny pomysł zainteresowanego.

### C. Zaawansowana automatyzacja dokumentów

Dane wprowadzone raz są wykorzystywane wszędzie.

### D. Lepsza dokumentacja fotograficzna

W tym obsługa fotografii wykonanych zewnętrznym aparatem.

### E. Rysowanie zaleceń bezpośrednio na zdjęciach

Eliminacja części pracy wykonywanej obecnie w ZWCAD.

### F. Automatyczna symbolika działań

Zalecenie wpływa bezpośrednio na mapę.

### G. Elastyczne formaty wymiany

QGIS, gminy, projektanci, wykonawcy.

### H. Jeden model drzewa wykorzystywany w kolejnych opracowaniach

Jeżeli potwierdzi się potrzeba historii drzewa.

------------------------------------------------------------------------

## 26. Dalszy rozwój

W pierwszej rozmowie pojawił się pomysł, że mechanizm może później wyjść
poza klasyczną arborystykę.

Wspomniano m.in.:

-   inwentaryzacje przyrodnicze,
-   ornitologiczne,
-   fitosanitarne,
-   potencjalnie inne branże.

Technicznie ma to sens, jeśli od początku stworzymy:

**mapa + obiekt + konfigurowalny formularz + multimedia + geometria +
raport**

zamiast systemu zakodowanego wyłącznie jako:

`TREE(id, species, circumference...)`.

Ale **nie powinno to obecnie rozszerzać MVP**. Najpierw system powinien
dobrze rozwiązać rzeczywisty problem arborysty.

------------------------------------------------------------------------

## 27. Najważniejsze rzeczy, których jeszcze potrzebujemy

To jest teraz kluczowa część całego konspektu.

### Proces

Trzeba poznać kolejne etapy pracy równie dokładnie, jak opisany został
początek zlecenia.

### Model danych

Potrzebna jest pełna lista informacji przechowywanych dla drzewa i
innych obiektów.

### Waloryzacja

Trzeba poznać metody, skale, kryteria i wynik.

### Gospodarka drzewostanem

Trzeba ustalić, jak z danych wejściowych powstają zalecenia.

### QGIS

Trzeba poznać realny workflow:

**co trafia do QGIS → co jest tam robione → co z niego wychodzi.**

### CAD

Analogicznie:

**co dokładnie wymaga dzisiaj ZWCAD i dlaczego.**

### GNSS

Potrzebny jest model urządzenia, sposób komunikacji, RTK/NTRIP, wymagana
dokładność i działanie bez internetu.

### Fotografie

Trzeba ustalić oczekiwany workflow z zewnętrznym aparatem.

### Dokumentacja

Potrzeba kilku różnych prawdziwych przykładów:

-   inwentaryzacji,
-   waloryzacji,
-   gospodarki,
-   programu konserwatorskiego,
-   ochrony drzew na budowie.

### Format danych klientów

Potrzebne są przykłady realnych plików wejściowych i wyjściowych.

### Gminy

Trzeba ustalić, jakie konkretnie formaty i struktury są wymagane oraz do
jakich systemów dane rzeczywiście trafiają.

### Historia drzewa

Nie wiadomo jeszcze, czy system ma przechowywać drzewo tylko w ramach
pojedynczego projektu, czy tworzyć **długoterminową kartę drzewa**.

To jest bardzo ważna decyzja architektoniczna.

------------------------------------------------------------------------

## 28. Obecna wizja systemu w jednym schemacie

``` text
                       PROJEKT / ZLECENIE
                              │
              ┌───────────────┼───────────────┐
              │               │               │
        DANE ŹRÓDŁOWE        MAPA          DOKUMENTY
              │               │
              │        ┌──────┴──────┐
              │        │             │
              │      DRZEWA      INNE OBIEKTY
              │        │
              │        ├── lokalizacja GNSS
              │        ├── gatunek
              │        ├── pomiary
              │        ├── stan
              │        ├── ocena
              │        ├── fotografie
              │        ├── waloryzacja
              │        └── zalecenia
              │
              └──────────────┐
                             ↓
                     OPRACOWANIE DANYCH
                             │
              ┌──────────────┼──────────────┐
              ↓              ↓              ↓
         INWENTARYZACJA  WALORYZACJA    GOSPODARKA
              │              │              │
              └──────────────┼──────────────┘
                             ↓
                     MAPY / TABELE / FOTO
                             ↓
                     RAPORT / DOKUMENTACJA
                             ↓
                ┌────────────┼─────────────┐
                ↓            ↓             ↓
              PDF         GIS/QGIS      XLSX/DXF/...
```

------------------------------------------------------------------------

## 29. Najważniejszy wniosek na obecnym etapie

Po zebranych materiałach **nie należy projektować jedynie „aplikacji do
inwentaryzacji drzew"**.

To byłoby za wąskie i w dużej mierze powielałoby ArboMap.

Projekt zaczyna wyglądać raczej jak:

> **system do prowadzenia projektu arborystycznego od materiałów
> źródłowych i inwentaryzacji terenowej, przez ocenę i planowanie
> działań, aż do wygenerowania map, tabel i kompletnej dokumentacji dla
> klienta.**

ArboMap może być wzorcem bardzo dobrej obsługi terenu, QGIS pozostaje
wyspecjalizowanym narzędziem GIS, a CAD może pozostać narzędziem do
zaawansowanych prac graficznych.

Wartość nowego rozwiązania może leżeć przede wszystkim **pomiędzy
nimi**: w spięciu całego procesu i usunięciu ręcznego przepisywania,
konwertowania, składania dokumentacji i powtarzania tych samych
czynności.

Na tym etapie **nie ma jeszcze wystarczających informacji, żeby
rozpocząć kodowanie właściwego produktu**. Jest natomiast wystarczająco
dużo materiału, żeby rozpocząć projektowanie domeny, zaznaczyć
niewiadome i na podstawie kolejnych odpowiedzi dojść do sensownego MVP.

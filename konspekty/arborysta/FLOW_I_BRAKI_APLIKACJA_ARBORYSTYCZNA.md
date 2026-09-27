# FLOW PROJEKTU I BRAKUJĄCE INFORMACJE

## Zintegrowane narzędzie arborystyczne - stan koncepcji

> Dokument roboczy uzupełniający
> `KONSPEKT_APLIKACJI_ARBORYSTYCZNEJ.md`.\
> Pokazuje obecnie rozpoznany przepływ pracy oraz informacje, których
> nadal brakuje przed zamknięciem wymagań i MVP.

------------------------------------------------------------------------

## 1. Główny flow

``` mermaid
flowchart TD
    A[Klient / zlecenie] --> B[Pozyskanie materiałów źródłowych]
    B --> B1[DWG / DXF]
    B --> B2[PDF / rastry]
    B --> B3[Dane o obiekcie, ochronie, zabytkach]
    B --> C[Utworzenie projektu]

    C --> D[Przygotowanie mapy i formularzy]
    D --> E[Praca terenowa]

    E --> E1[Lokalizacja drzewa]
    E1 --> E2[GNSS / RTK lub ręczne wskazanie]
    E2 --> E3[Nadanie ID]

    E3 --> F[Karta drzewa]
    F --> F1[Gatunek]
    F --> F2[Pomiary]
    F --> F3[Stan zdrowotny / statyka]
    F --> F4[Zagrożenia]
    F --> F5[Waloryzacja / ocena]
    F --> F6[Zalecenia i typy prac]
    F --> F7[Zdjęcia / nagrania / notatki]

    F --> G[Centralny model danych projektu]

    G --> H1[Inwentaryzacja]
    G --> H2[Waloryzacja]
    G --> H3[Gospodarka drzewostanem]
    G --> H4[Program prac konserwatorskich]
    G --> H5[Ochrona drzew na budowie]

    H1 --> I[Automatyczne opracowanie]
    H2 --> I
    H3 --> I
    H4 --> I
    H5 --> I

    I --> I1[Tabele]
    I --> I2[Mapy i symbole]
    I --> I3[Strefy / geometrie]
    I --> I4[Fotografie i szkice cięć]
    I --> I5[Opis / raport]

    I1 --> J[Dokumentacja końcowa]
    I2 --> J
    I3 --> J
    I4 --> J
    I5 --> J

    J --> K1[PDF]
    J --> K2[XLSX / CSV]
    J --> K3[GIS: GeoPackage / SHP]
    J --> K4[DXF / wymiana z CAD]
    J --> K5[QGIS / systemy klienta]
```

------------------------------------------------------------------------

## 2. Flow w uproszczeniu

``` text
KLIENT / ZLECENIE
        |
        v
MATERIAŁY ŹRÓDŁOWE
DWG / PDF / GIS / informacje formalne
        |
        v
PROJEKT
        |
        v
PRACA TERENOWA
        |
        +--> lokalizacja GNSS / RTK
        +--> ID drzewa
        +--> gatunek
        +--> pomiary
        +--> ocena stanu
        +--> zagrożenia
        +--> zdjęcia
        +--> zalecenia
        |
        v
CENTRALNA BAZA PROJEKTU
        |
        +--> inwentaryzacja
        +--> waloryzacja
        +--> gospodarka drzewostanem
        +--> program prac konserwatorskich
        +--> ochrona drzew na budowie
        |
        v
AUTOMATYCZNE OPRACOWANIE
        |
        +--> mapa
        +--> tabela
        +--> symbole prac
        +--> strefy
        +--> zdjęcia i szkice
        +--> treść dokumentu
        |
        v
DOKUMENTACJA KOŃCOWA
PDF / XLSX / CSV / GIS / DXF / QGIS
```

------------------------------------------------------------------------

# 3. Co już wiemy

## 3.1. Cel produktu

Nie chodzi wyłącznie o aplikację do inwentaryzacji drzew. Docelowo ma to
być narzędzie prowadzące projekt arborystyczny możliwie daleko od
początku zlecenia do gotowej dokumentacji.

Pierwotna potrzeba została opisana jako stworzenie **„kombajnu"**,
ponieważ obecnie używanych jest wiele niewspółgrających ze sobą
narzędzi.

## 3.2. Typowy proces terenowy

Rozpoznany proces wygląda następująco:

1.  Klient wskazuje drzewa lub drzewostan.
2.  Arborysta pozyskuje materiały źródłowe.
3.  Przygotowywane są mapy/podkłady.
4.  W terenie lokalizowane są drzewa.
5.  Drzewo otrzymuje ID.
6.  Określany jest gatunek.
7.  Wprowadzane są podstawowe wymiary.
8.  Oceniany jest stan zdrowotny i zagrożenia.
9.  Wprowadzane są zalecenia.
10. Wykonywana jest dokumentacja fotograficzna.
11. Po powrocie dane są przetwarzane w biurze.
12. Powstają tabele, mapy, opisy, fotografie i dokument końcowy.

## 3.3. ArboMap dobrze rozwiązuje część terenową

Warto zachować lub odtworzyć jego dobre elementy:

-   praca na mapie,
-   lokalizacja z GNSS,
-   szybkie tworzenie rekordu drzewa,
-   numerowanie,
-   formularze,
-   wprowadzanie tekstu głosem,
-   zdjęcia przypisywane do konkretnego drzewa,
-   eksport danych,
-   obsługa DXF, rastrów i WMS,
-   eksport m.in. do XLSX, CSV, PDF, DXF i SHP.

Nie ma sensu tworzyć produktu, który jedynie kopiuje te funkcje.

## 3.4. Największy widoczny problem znajduje się po zebraniu danych

Po pracy terenowej nadal występuje dużo ręcznych czynności.

Arborysta wskazał m.in.:

> „W biurze przetwarzam zebrane dane."

> „Tworzę dokument do moich potrzeb."

> „Obecnie dla moich potrzeb modyfikuję mapy o brakujące w arbo symbole
> prac, tabelki w zw cad."

> „W zw cad na fotografiach dorysowuję szkice cięć i umieszczam je w
> dokumencie."

To sugeruje, że bardzo duża wartość nowego systemu może leżeć w
automatyzacji etapu:

**dane terenowe -\> opracowanie -\> gotowa dokumentacja.**

## 3.5. Wiemy już o kilku rodzajach opracowań

-   program prac konserwatorskich,
-   inwentaryzacja drzewostanu,
-   waloryzacja drzew,
-   gospodarka drzewostanem,
-   program ochrony drzew na placu budowy.

Nie powinny to być osobne, niezależne systemy. Powinny korzystać z
jednego modelu projektu i danych.

------------------------------------------------------------------------

# 4. Czego nadal brakuje

## 4.1. Pełnego modelu danych drzewa

Nie znamy jeszcze kompletnego zestawu pól.

Wiemy o:

-   ID,
-   gatunku,
-   obwodzie,
-   wysokości,
-   średnicy korony,
-   stanie zdrowotnym,
-   stabilności/statyce,
-   zagrożeniach,
-   zaleceniach,
-   współrzędnych,
-   fotografiach.

Ale nie wiadomo:

-   które pola są zawsze obowiązkowe,
-   które zależą od rodzaju projektu,
-   jakie są słowniki wartości,
-   które pola są liczbami, tekstem, listami lub ocenami,
-   jakie dodatkowe dane stosowane są przy specjalistycznych ocenach.

**Potrzebne:** rzeczywiste formularze/karty z kilku różnych typów
zleceń.

------------------------------------------------------------------------

## 4.2. Dokładnego procesu waloryzacji

Wiemy, że waloryzacja może uwzględniać m.in.:

-   wiek,
-   wartość przyrodniczą,
-   wartość kulturową,
-   wartość krajobrazową.

Nie wiemy:

-   jakie istnieją metody,
-   czy są stosowane skale punktowe,
-   jakie dane wejściowe wpływają na wynik,
-   czy wynik można obliczyć automatycznie,
-   czy różne gminy/klienci wymagają innych metod,
-   jak wynik powinien być wizualizowany na mapie.

**Potrzebne:** przykład prawdziwej waloryzacji wraz z opisem sposobu jej
wykonania.

------------------------------------------------------------------------

## 4.3. Dokładnego procesu gospodarki drzewostanem

Nie znamy pełnej drogi:

**ocena drzewa -\> decyzja -\> zalecenie -\> symbol na mapie -\>
dokument.**

Trzeba ustalić:

-   katalog możliwych prac,
-   kody i skróty,
-   czy katalog jest stały,
-   czy użytkownik tworzy własne typy działań,
-   czy jedno drzewo może mieć wiele działań,
-   czy działania mają terminy, priorytety i status wykonania.

------------------------------------------------------------------------

## 4.4. Pełnego workflow QGIS

Wiemy, że QGIS jest istotny, ale nie wiemy jeszcze dokładnie:

-   co trafia do QGIS,
-   w jakim formacie,
-   jakie operacje wykonywane są tam ręcznie,
-   jakie warstwy są używane,
-   jakie style i symbole są stosowane,
-   co następnie jest eksportowane,
-   które czynności można przenieść do nowego systemu,
-   które powinny pozostać w QGIS.

**Potrzebne:** przejście krok po kroku przez jeden prawdziwy projekt
QGIS.

------------------------------------------------------------------------

## 4.5. Pełnego workflow CAD / ZWCAD

Wiemy już, że CAD jest używany do:

-   uzupełniania map,
-   brakujących symboli,
-   tabelek,
-   szkiców cięć na fotografiach.

Nie wiadomo jednak:

-   jakie jeszcze czynności są wykonywane,
-   które są niezbędne,
-   które są tylko obejściem ograniczeń ArboMap,
-   jaki format danych przechodzi między ArboMap, QGIS i CAD.

**Potrzebne:** lista czynności wykonywanych w ZWCAD przy typowym
zleceniu.

------------------------------------------------------------------------

## 4.6. Szczegółów GNSS / RTK

Nie znamy jeszcze:

-   dokładnego modelu używanego odbiornika,
-   sposobu komunikacji z tabletem,
-   konfiguracji NTRIP,
-   wykorzystania ASG-EUPOS,
-   wymaganej dokładności,
-   sposobu kontroli Fix/Float,
-   zachowania przy utracie internetu,
-   zachowania przy utracie korekt,
-   sposobu ręcznej korekty punktu.

**Potrzebne:** dokładny opis jednego pomiaru drzewa od uruchomienia
sprzętu do zapisania punktu.

------------------------------------------------------------------------

## 4.7. Pracy offline

To krytyczne dla aplikacji terenowej, ale nie mamy jeszcze
potwierdzonego wymagania.

Trzeba ustalić:

-   czy w terenie występuje brak internetu,
-   które mapy muszą działać offline,
-   czy formularze muszą działać offline,
-   czy zdjęcia są zapisywane lokalnie,
-   kiedy następuje synchronizacja,
-   jak rozwiązywać konflikty danych.

------------------------------------------------------------------------

## 4.8. Integracji z zewnętrznym aparatem

Wiemy, że obecne rozwiązanie jest niewystarczające:

> „brak tu możliwości współpracy z dobrym aparatem foto - szkoda"

Nie wiadomo jednak, jaki workflow byłby najlepszy.

Możliwości:

-   import zdjęć po zakończeniu pracy,
-   automatyczne przypisanie według czasu/GPS,
-   kod/QR identyfikujący drzewo,
-   ręczne przypisywanie,
-   bezpośrednia komunikacja aparatu z aplikacją.

**Potrzebne:** opis idealnego procesu wykonywania zdjęć.

------------------------------------------------------------------------

## 4.9. Szkiców na fotografiach

Wiemy, że obecnie szkice cięć są wykonywane w ZWCAD.

Nie wiadomo:

-   jakie narzędzia graficzne są rzeczywiście potrzebne,
-   czy wystarczą linie i strzałki,
-   czy potrzebne są kolory,
-   warstwy,
-   tekst,
-   symbole,
-   możliwość cofania,
-   wersjonowanie szkicu.

To może być mały moduł, który usuwa cały etap pracy w CAD.

------------------------------------------------------------------------

## 4.10. Szablonów dokumentów

Nie znamy jeszcze pełnej struktury wszystkich dokumentów.

Potrzebne są rzeczywiste przykłady:

-   programu prac konserwatorskich,
-   inwentaryzacji,
-   waloryzacji,
-   gospodarki drzewostanem,
-   programu ochrony drzew na budowie.

Dopiero wtedy można określić:

-   części stałe,
-   części automatyczne,
-   części wymagające ręcznego opisu,
-   tabele,
-   mapy,
-   fotografie,
-   załączniki.

------------------------------------------------------------------------

## 4.11. Wymagań gmin i innych klientów

Pierwotnym założeniem była łatwa wymiana danych z gminami i
projektantami.

Nie znamy jednak jeszcze:

-   konkretnych systemów używanych przez odbiorców,
-   wymaganych formatów,
-   wymaganych nazw pól,
-   układów współrzędnych,
-   schematów plików,
-   sposobu importu danych przez urząd.

**Potrzebne:** 2-3 przykłady rzeczywistych wymagań od różnych
klientów/gmin.

------------------------------------------------------------------------

## 4.12. Historii drzewa

To ważna decyzja architektoniczna.

Nie wiadomo, czy:

**drzewo istnieje tylko w ramach konkretnego projektu**

czy:

**drzewo jest trwałym obiektem, który ma kolejne oględziny, badania,
zdjęcia, zabiegi i projekty.**

Drugi wariant prowadzi do systemu zarządzania drzewostanem w czasie i
jest znacznie większym zakresem.

To trzeba rozstrzygnąć przed zaprojektowaniem modelu danych.

------------------------------------------------------------------------

## 4.13. Pracy wielu użytkowników

Nie wiadomo:

-   czy przy jednym projekcie pracuje kilka osób,
-   czy kilka osób pracuje jednocześnie w terenie,
-   czy potrzebne są konta użytkowników,
-   role i uprawnienia,
-   historia zmian,
-   synchronizacja,
-   blokowanie edycji.

------------------------------------------------------------------------

## 4.14. Zakresu pierwszej wersji

Najważniejsza niewiadoma produktowa.

Trzeba ustalić, co musi znaleźć się w **MVP**, aby arborysta mógł
wykonać jedno prawdziwe zlecenie od początku do końca.

Bez tego łatwo zbudować jednocześnie:

-   GIS,
-   CAD,
-   system dokumentów,
-   aplikację terenową,
-   CRM,
-   system zarządzania zielenią,

czyli projekt zbyt duży na pierwszą wersję.

------------------------------------------------------------------------

# 5. Najważniejsze pytania do zamknięcia przed kodowaniem

Jeżeli mielibyśmy sprowadzić wszystkie braki do najważniejszych decyzji,
potrzebujemy odpowiedzi przede wszystkim na te kwestie:

1.  **Jaki dokładnie proces ma obsługiwać pierwsza wersja od początku do
    końca?**
2.  **Jaki typ zlecenia wybieramy jako pierwszy przypadek
    referencyjny?**
3.  **Jak wygląda kompletna karta drzewa dla tego zlecenia?**
4.  **Co dokładnie po terenie wykonywane jest w QGIS, ZWCAD, Excelu i
    edytorze dokumentów?**
5.  **Które z tych czynności nowa aplikacja ma wyeliminować?**
6.  **Jakie dokumenty i pliki muszą powstać na końcu?**
7.  **Jak działa waloryzacja i gospodarka drzewostanem?**
8.  **Jakie geometrie poza punktem drzewa muszą być obsługiwane?**
9.  **Jak dokładnie ma działać GNSS/RTK i tryb offline?**
10. **Czy drzewo ma historię niezależną od pojedynczego projektu?**
11. **Jakie formaty/systemy klientów są naprawdę wymagane?**
12. **Jak ma działać dokumentacja fotograficzna z dobrego aparatu?**
13. **Jakie symbole, legendy i style mapowe trzeba automatyzować?**
14. **Czy pracuje jedna osoba czy zespół?**
15. **Jakie 5-10 funkcji jest absolutnie koniecznych w pierwszej
    używalnej wersji?**

------------------------------------------------------------------------

# 6. Obecna hipoteza MVP

Na dziś najbardziej sensownym kandydatem na pierwszy pełny scenariusz
wydaje się:

**Program prac konserwatorskich dla drzew**

ponieważ jest to wskazany jako najczęstszy rodzaj zlecenia i jego
workflow został już częściowo opisany.

Hipotetyczne MVP:

``` text
UTWÓRZ PROJEKT
      |
      v
DODAJ PODKŁAD / MAPĘ
      |
      v
PRACA TERENOWA
GNSS + karta drzewa + głos + zdjęcia
      |
      v
ZALECENIA / TYPY PRAC
      |
      v
AUTOMATYCZNA MAPA + TABELA
      |
      v
PRZYPISANIE / OBRÓBKA ZDJĘĆ
      |
      v
GENERATOR PROGRAMU PRAC
      |
      v
PDF + XLSX + GIS / DXF
```

To jest **hipoteza**, nie zatwierdzony zakres.

------------------------------------------------------------------------

# 7. Najważniejsza zasada projektowa

**Dane wprowadzamy raz.**

Jeżeli informacja o drzewie zostanie zmieniona, zmiana powinna być
automatycznie widoczna wszędzie tam, gdzie ta informacja jest używana:

``` text
KARTA DRZEWA
     |
     +--> TABELA
     +--> MAPA
     +--> LEGENDA
     +--> RAPORT
     +--> EKSPORT GIS
```

To może być jedna z największych przewag systemu nad obecnym,
rozproszonym procesem.

------------------------------------------------------------------------

# 8. Stan projektu

### Wiemy wystarczająco dużo, aby:

-   rozumieć główny problem,
-   zarysować domenę,
-   wskazać główne moduły,
-   rozpoznać dobre elementy ArboMap,
-   znaleźć miejsca obecnego ręcznego procesu,
-   przygotować dalsze pytania,
-   wybrać kandydatów do MVP.

### Nie wiemy jeszcze wystarczająco dużo, aby:

-   zamknąć model bazy danych,
-   wybrać finalną architekturę,
-   zamknąć zakres MVP,
-   projektować szczegółowe API,
-   projektować finalne ekrany,
-   rozpocząć implementację właściwego produktu bez ryzyka dużych
    przeróbek.

Następnym krokiem powinno być zebranie pozostałych odpowiedzi i
przejście z arborystą przez **jeden rzeczywisty projekt od plików
wejściowych aż do finalnego PDF-u**. To powinno dostarczyć materiału do
pierwszej pełnej specyfikacji funkcjonalnej.

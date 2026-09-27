Tak. Na ten moment mamy już wystarczająco dużo, żeby **zacząć budować fundament systemu**, ale jeszcze zdecydowanie za mało, żeby zamknąć funkcjonalny MVP i zacząć implementować wszystkie procesy domenowe.

## Stan projektu na dziś

Najważniejsze jest to, że przestaliśmy myśleć o produkcie jako o „lepszym ArboMapie”. Coraz bardziej klaruje się system do **prowadzenia projektu arborystycznego od materiałów źródłowych i pracy terenowej aż po gotową dokumentację i wymianę danych**.

Roboczo:

**Projekt → materiały/mapa → obiekty/drzewa → praca terenowa → ocena → zalecenia/waloryzacja → mapy/tabele/zdjęcia → dokumentacja → eksport/integracje**

Jednocześnie pojawił się drugi potencjalny scenariusz:

**Urząd → tworzy Projekt / zakres → przekazuje wykonawcy → arborysta realizuje → urząd odbiera dane → dane pozostają w systemie**

Ten drugi scenariusz jest na razie **hipotezą produktową**. Właśnie dlatego uruchomiliśmy Deep Research dotyczący funkcjonowania polskich urzędów, przetargów i zarządzania zielenią. Nie powinniśmy jeszcze projektować architektury wokół założenia, że urząd rzeczywiście będzie pracował dokładnie w taki sposób.

---

# 1. Co już wiemy

### Centralną encją powinien być Projekt

To jest obecnie jeden z najmocniejszych elementów modelu.

Projekt reprezentuje konkretne zlecenie / opracowanie. W jego ramach znajdują się między innymi:

- materiały źródłowe,
- mapa,
- drzewa i potencjalnie inne obiekty przestrzenne,
- pomiary,
- oceny,
- zdjęcia,
- zalecenia,
- dane GIS,
- dokumenty,
- eksporty.

Arborysta musi móc utworzyć Projekt samodzielnie. **System nie może wymagać istnienia urzędu, zamawiającego ani zewnętrznego workflow.**

To powinien być podstawowy przypadek użycia.

---

# 2. Wiemy, jakie rodzaje pracy wykonuje ekspert

Z odpowiedzi klienta mamy już kilka rzeczywistych typów opracowań:

**Program prac konserwatorskich** — według klienta najczęstszy rodzaj zlecenia.

Ponadto:

- inwentaryzacja drzewostanu,
- waloryzacja drzew,
- gospodarka drzewostanem,
- program ochrony drzew na placu budowy.

I mamy ważne rozróżnienie domenowe:

> „inwentaryzacja jest dokumentem wyjściowym, informacyjnym”

> „waloryzacja rozróżnia drzewa pod względem wieku, wartości przyrodniczej, kulturowej, krajobrazowej”

> „gospodarka drzewostanem jest planem podstawowych prac przy drzewach.”

To sugeruje, że nie powinniśmy budować pięciu niezależnych aplikacji/modułów danych. Bardziej prawdopodobne jest:

**jeden Projekt + jeden wspólny model drzewa + różne rodzaje opracowań wykorzystujące te dane.**

To jednak nadal wymaga potwierdzenia na większej liczbie rzeczywistych dokumentów.

---

# 3. Znamy rzeczywisty workflow arborysty

Mamy już pierwszy konkretny proces.

Klient otrzymuje materiały, np.:

**DWG / PDF / mapy geodezyjne / informacje o obiekcie.**

Następnie przygotowuje się do pracy terenowej.

W terenie lokalizuje drzewa:

- na podstawie map,
- ręcznie,
- albo przy użyciu ArboMap + GNSS.

Dla drzewa zbiera m.in.:

- ID,
- gatunek,
- podstawowe wymiary,
- stan zdrowotny,
- zagrożenia,
- zalecenia.

Wykonuje dokumentację fotograficzną.

ArboMap pozwala przypisać fotografie bezpośrednio do drzewa oraz korzystać z wprowadzania głosowego.

Potem:

> „W biurze przetwarzam zebrane dane.”

I właśnie tutaj zaczyna się duża część problemu.

Dane przechodzą dalej przez różne narzędzia.

W uproszczeniu obecny proces wygląda mniej więcej tak:

**ArboMap → XLS → CAD/ZWCAD / inne narzędzia → zdjęcia → dokument → PDF**

Klient mówi wprost:

> „Obecnie dla moich potrzeb modyfikuję mapy o brakujące w arbo symbole prac, tabelki w zw cad.”

oraz:

> „W zw cad na fotografiach dorysowuję szkice cięć i umieszczam je w dokumencie.”

To jest bardzo ważny obszar potencjalnej automatyzacji.

---

# 4. Wiemy, że mapa jest częścią rdzenia produktu

Mapa nie może być dodatkiem.

Drzewo jest obiektem przestrzennym.

Powinniśmy od początku zakładać co najmniej model:

**Projekt → mapa → obiekty przestrzenne → drzewo**

Drzewo ma lokalizację.

Prawdopodobnie będziemy również potrzebowali:

- punktów,
- linii,
- poligonów,
- stref,
- etykiet,
- symboli.

Na przykład w ochronie drzew na placu budowy pojawiają się strefy ochronne i obszary wyłączenia.

Nie znamy jeszcze jednak dokładnego katalogu geometrii i zachowań.

---

# 5. Wiemy, że drzewo będzie jedną z głównych encji domenowych

Możemy już bezpiecznie przewidzieć coś w rodzaju:

```
Project
   |
   +-- Tree
         |
         +-- Location
         +-- Measurements
         +-- Assessments
         +-- Photos
         +-- Recommendations
         +-- Valuation
```

Nie należy jeszcze kodować wszystkich pól na sztywno.

Wiemy, że pojawiają się m.in.:

- identyfikator/numer,
- gatunek,
- nazwa łacińska,
- obwód/pień,
- wysokość,
- korona,
- stan,
- zagrożenia,
- zalecenia,
- lokalizacja,
- fotografie.

Ale **nie mamy jeszcze finalnego modelu karty drzewa**.

---

# 6. Wiemy, że formularze muszą być elastyczne

To jest istotne również dlatego, że ArboMap już posiada konfigurowalne pytania i odpowiedzi.

Różne:

- rodzaje opracowania,
- urzędy,
- klienci,
- metodyki,

mogą wymagać innych informacji.

Dlatego nie projektowałbym systemu jako:

```
class Tree {
    String field1;
    String field2;
    ...
    String field87;
}
```

i uznania sprawy za zakończoną.

Potrzebujemy kombinacji:

**stabilnego rdzenia domenowego + rozszerzalnych formularzy/atrybutów.**

Dokładny mechanizm można jeszcze ustalić później.

---

# 7. Wiemy, że zdjęcia są bardzo ważne

To nie jest zwykły attachment.

Fotografia jest częścią dokumentacji konkretnego drzewa.

Klient wskazał zarówno zaletę ArboMap:

> „fotografie z tabletu przypisywane są dla konkretnego drzewa.”

jak i problem:

> „brak tu możliwości współpracy z dobrym aparatem foto - szkoda”

Do tego dochodzi obecne rysowanie na fotografiach w ZWCAD.

Dlatego możemy już przewidzieć model:

```
Tree
 └── Media
      ├── Photo
      ├── description
      ├── category
      └── annotations
```

Natomiast **integracji z zewnętrznym aparatem jeszcze nie projektowałbym technicznie**. Najpierw trzeba ustalić, jak ekspert chciałby z niego korzystać.

---

# 8. Wiemy, że głos jest realnym elementem workflow

To nie jest pomysł wymyślony przez nas.

Klient konkretnie chwali tę funkcję:

> „Arbo tę część pracy mocno ułatwia przez zapisywanie w kolejnych polach tekstu przetworzonego z mowy.”

Dlatego interfejs terenowy powinien być od początku projektowany tak, żeby pole formularza mogło być uzupełniane również głosem.

Nie musimy natomiast implementować własnego speech-to-text w pierwszej wersji backendu.

---

# 9. Wiemy, że QGIS powinien być partnerem, a nie konkurentem

Nie ma sensu pisać własnego QGIS-a.

Docelowa relacja powinna wyglądać raczej:

**nasza aplikacja ↔ QGIS**

Nasza aplikacja specjalizuje się w arborystyce.

QGIS pozostaje narzędziem do zaawansowanych operacji GIS.

Musimy natomiast doprowadzić do sytuacji, w której wymiana danych między nimi jest bardzo dobra.

Nie wiemy jeszcze, czy optymalnym rezultatem będzie:

- GeoPackage,
- SHP,
- GeoJSON,
- QGIS project,
- style QML,
- czy jakaś kombinacja.

To wymaga poznania faktycznego workflow eksperta.

---

# 10. Wiemy, czego NIE powinniśmy budować

Nie budujemy:

**QGIS 2.0.**

Nie budujemy:

**AutoCAD/ZWCAD 2.0.**

Nie budujemy również:

**ArboMap clone.**

Powinniśmy natomiast przejąć te proste, powtarzalne operacje, przez które arborysta obecnie musi otwierać inne programy.

Przykład:

```
zalecenie dla drzewa
        ↓
symbol na mapie
        ↓
legenda
        ↓
wiersz tabeli
        ↓
opis w dokumentacji
```

Jeżeli tę samą informację ekspert obecnie ręcznie przenosi cztery razy, to jest dokładnie rodzaj problemu, który system powinien eliminować.

---

# Co możemy już zacząć kodować

Możemy zacząć od **fundamentu technicznego**, a nie od kompletnego procesu biznesowego.

### 1. Project

Możemy utworzyć podstawową encję:

```
Project

id
name
description
type
status
createdAt
updatedAt
```

Ale `type` i `status` traktowałbym jeszcze ostrożnie — wartości prawdopodobnie będą ewoluować.

### 2. Użytkownik i właściciel projektu

Możemy przygotować:

```
User
Organization
ProjectMembership
```

żeby architektura nie zakładała, że projekt zawsze należy do pojedynczej osoby.

Dzięki temu później możliwe będzie:

```
Przemek / arborysta
       ↓
     Project
```

ale również:

```
Urząd
  ↓
Project
  ↓
Arborysta / firma
```

bez przebudowy całego systemu.

### 3. Podstawowa encja Tree

Możemy stworzyć jej **minimalny rdzeń**, nie pełną kartę.

Np.:

```
Tree

id
projectId
inventoryNumber
species
geometry/location
createdAt
updatedAt
```

Pozostałych kilkudziesięciu potencjalnych pól jeszcze bym nie utrwalał.

### 4. Geometria

Już teraz warto budować system z myślą o danych przestrzennych.

Jeżeli backend będzie Spring Boot + PostgreSQL, sensownym kierunkiem jest PostgreSQL + PostGIS.

To fundamentalna decyzja, która pasuje do tego produktu dużo lepiej niż traktowanie współrzędnych jako dwóch przypadkowych `double`.

### 5. Attachment / Media

Możemy przygotować ogólny system plików:

```
Project
 └── Attachment
```

oraz:

```
Tree
 └── Media
```

bez implementowania jeszcze wszystkich funkcji fotograficznych.

### 6. Historia zmian

Warto od początku mieć:

```
createdBy
createdAt
updatedBy
updatedAt
```

i przygotować architekturę pod wersjonowanie/audyt.

Przy potencjalnym B2G może się to później okazać bardzo istotne.

### 7. API i podstawowy frontend projektu

Możemy spokojnie zrobić:

**logowanie → lista projektów → utworzenie projektu → otwarcie projektu → mapa → lista drzew → podstawowa karta drzewa.**

To już da działający szkielet produktu.

---

# Czego jeszcze NIE kodowałbym

Nie kodowałbym teraz kompletnej **waloryzacji**.

Nie znamy metod, skal, algorytmów i zakresu eksperckiej oceny.

Nie kodowałbym kompletnej **gospodarki drzewostanem**.

Nie znamy dokładnych zależności pomiędzy oceną a zaleceniami.

Nie kodowałbym finalnego **generatora raportów**.

Najpierw musimy rozłożyć kilka prawdziwych dokumentów na części i ustalić:

**dane → tabela → mapa → zdjęcia → tekst → finalny dokument.**

Nie kodowałbym jeszcze finalnej **integracji GNSS**.

Nie znamy konkretnego urządzenia/protokołu i rzeczywistego sposobu pracy.

Nie kodowałbym integracji z **aparatem fotograficznym**.

Nie znamy jeszcze oczekiwanego workflow.

Nie kodowałbym pełnego **import/export engine**.

Możemy zaprojektować architekturę adapterów, ale nie ma sensu implementować dziesięciu formatów, zanim nie wiemy, których naprawdę potrzebują użytkownicy.

I przede wszystkim nie kodowałbym jeszcze rozbudowanego **modułu urzędu**.

---

# Największe rzeczy wymagające doprecyzowania

Najważniejsze pytania dotyczą teraz nie technologii, tylko domeny.

Musimy dokładnie poznać:

**1. Kartę drzewa** — wszystkie informacje, które mogą być zbierane i które z nich są wspólne dla różnych opracowań.

**2. Waloryzację** — jak dokładnie ekspert ją wykonuje, jakie są skale, kryteria i obliczenia.

**3. Gospodarkę drzewostanem** — jak powstają zalecenia i jakie dane są wynikiem.

**4. Symbole prac** — dokładnie czego brakuje w ArboMap i co ekspert dodaje w ZWCAD.

**5. QGIS** — trzeba zobaczyć rzeczywisty projekt QGIS eksperta.

**6. GNSS** — model urządzenia, komunikacja, RTK, dokładność i praca pod koronami.

**7. Fotografie** — sposób organizowania zdjęć oraz oczekiwana współpraca z zewnętrznym aparatem.

**8. Dokumenty** — które fragmenty są powtarzalnym szablonem, a które ekspert zawsze pisze indywidualnie.

**9. Formaty wymiany** — nie hipotetyczne, tylko takie, które rzeczywiście otrzymuje i musi oddawać.

**10. Urzędy** — jak faktycznie zlecają, przekazują i odbierają takie prace. To właśnie ma nam teraz ustalić Deep Research.

---

# Co jest teraz najcenniejszym materiałem do zdobycia

Nie kolejna lista funkcji.

Potrzebujemy **jednego kompletnego, zanonimizowanego rzeczywistego zlecenia**.

Najlepiej:

```
1. To dostałem od klienta/urzędu.

2. To przygotowałem przed wyjazdem.

3. Z tym pojechałem w teren.

4. To zebrałem w ArboMap.

5. To wyeksportowałem.

6. To zrobiłem w QGIS.

7. To poprawiłem w ZWCAD.

8. To zrobiłem ze zdjęciami.

9. Tak powstała tabela.

10. Tak powstała mapa.

11. Tak powstał raport.

12. To finalnie dostał klient.
```

Jeżeli ekspert przejdzie z nami przez **jeden projekt od początku do końca**, możemy na jego podstawie stworzyć pierwszy prawdziwy model domenowy.

---

# Proponowany podział prac od teraz

Możemy prowadzić dwa tory równolegle.

**Tor techniczny** może już wystartować:

```
Spring Boot
PostgreSQL + PostGIS
        ↓
User / Organization
        ↓
Project
        ↓
ProjectMember
        ↓
Tree
        ↓
Geometry
        ↓
Attachment / Media
        ↓
Audit
        ↓
REST API
        ↓
podstawowy frontend + mapa
```

Natomiast **tor domenowy** nadal prowadzimy discovery:

```
kolejne odpowiedzi arborysty
        +
realne dokumenty
        +
ArboMap
        +
workflow QGIS/ZWCAD
        +
badanie urzędów/przetargów
        ↓
model danych
        ↓
workflow
        ↓
MVP
        ↓
generator dokumentacji
        ↓
integracje
```

Te dwa tory mogą iść równolegle.

## Najważniejsza granica

**Możemy już kodować platformę.**

**Nie powinniśmy jeszcze kodować naszych wyobrażeń o pracy arborysty.**

To rozróżnienie pozwoli nam teraz ruszyć technicznie bez sytuacji, w której po kolejnych odpowiedziach eksperta trzeba wyrzucić połowę aplikacji.

Po otrzymaniu wyników Deep Research dotyczącego urzędów dołożymy trzecią warstwę: **co potwierdził arborysta → co wynika z realnych zamówień publicznych → gdzie oba procesy się przecinają**. Dopiero wtedy będzie dobry moment na zamrożenie zakresu **MVP v1**.
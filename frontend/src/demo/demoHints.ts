// Centralny zbior tresci trybu demo: wskazowki przy ikonach (i) i komunikaty zablokowanych funkcji.
// Teksty opisuja tylko funkcje, ktore faktycznie istnieja w aplikacji (DOKUMENTACJA_FUNKCJONALNOSCI.md);
// nowa funkcja = nowy wpis tutaj, a nie tekst wpisany w widok.

export interface DemoHintContent {
  title: string;
  /** do czego sluzy i jak arborysta uzywa tego w praktyce - 1-2 zdania */
  body: string;
  /** co mozna zobaczyc albo zrobic w trybie demo */
  inDemo?: string;
  /** funkcja dziala w pelnej wersji, a w demo jest zablokowana (zmienia dane) */
  lockedInDemo?: boolean;
  /** dluzsze wyjasnienie w zwijanym panelu "Więcej" */
  more?: string;
}

export const DEMO_HINTS = {
  'demo.about': {
    title: 'Tryb demo',
    body: 'Przykładowe projekty arborysty z fikcyjnymi danymi. Możesz otwierać projekty, mapy, karty drzew, zdjęcia i pobierać dokumenty.',
    inDemo: 'Zmiany danych nie są zapisywane – przyciski edycji wyjaśniają, co robią w pełnej wersji.',
    more: 'Ikony (i) przy funkcjach można wyłączyć przyciskiem „Wskazówki”. Oprowadzanie krok po kroku uruchomisz przyciskiem „Oprowadź mnie”.',
  },
  'projects.list': {
    title: 'Projekty',
    body: 'Projekt to jedno zlecenie, np. inwentaryzacja parku albo ocena alei. Arborysta widzi tylko własne projekty.',
    inDemo: 'Otwórz dowolny z czterech projektów pokazowych – każdy pokazuje inny scenariusz pracy.',
  },
  'projects.new': {
    title: 'Nowy projekt',
    body: 'Tworzy nowe zlecenie z nazwą, opisem i danymi zlecenia (zleceniodawca, adres obiektu, data, numer umowy).',
    lockedInDemo: true,
  },
  'project.order': {
    title: 'Dane zlecenia',
    body: 'Zleceniodawca, adres obiektu, data zlecenia, numer umowy, cel opracowania i podstawa prawna trafiają do dokumentu PDF.',
    inDemo: 'Przycisk „Edytuj projekt” pokazuje, co można zmienić – zapis jest wyłączony.',
  },
  'project.summary': {
    title: 'Podsumowanie projektu',
    body: 'Liczby drzew, zdjęć, prac, waloryzacji, kontroli i grup drzew w jednym miejscu – szybki obraz postępu zlecenia.',
    inDemo: 'Przejdź do zakładek powyżej: Drzewa, Mapa, Dokumentacja…',
  },
  'trees.list': {
    title: 'Lista drzew i filtry',
    body: 'Wszystkie drzewa projektu z filtrami: waloryzacja, stan z ostatniej kontroli, oznaczenie, grupa drzew, iglaste / liściaste.',
    inDemo: 'Spróbuj filtra „Stan (ostatnia kontrola)” w projekcie z aleją lipową. Drzewa bez kontroli mają oznaczenie „Nie sprawdzone”.',
  },
  'tree.data': {
    title: 'Dane drzewa',
    body: 'Numer inwentaryzacyjny, gatunek (polski i łaciński), obwód pnia, wysokość, średnica korony oraz opisy stanu i zagrożeń. Formularz jest dostosowany do tabletu.',
    inDemo: 'Możesz przeglądać pola i wpisywać tekst, ale zapis jest wyłączony.',
    more: 'Pola tekstowe można wypełniać głosem (rozpoznawanie mowy w przeglądarce, wymaga HTTPS i zgody na mikrofon).',
  },
  'tree.location': {
    title: 'Lokalizacja i GPS',
    body: 'Punkt drzewa ustawia się kliknięciem na mapie, wpisaniem współrzędnych albo z GPS tabletu (pojedynczy odczyt lub średnia z serii).',
    inDemo: 'Zobacz źródło, dokładność i czas pomiaru zapisane przy drzewie.',
    more: 'Dokładność to wartość zgłoszona przez urządzenie. Aplikacja nie podaje typu rozwiązania RTK – przeglądarka go nie zna. Zewnętrzne odbiorniki GNSS (Bluetooth) nie są obsługiwane.',
  },
  'tree.markings': {
    title: 'Oznaczenia drzewa',
    body: 'Znaczniki z katalogu, np. pomnik przyrody, do usunięcia, dziupla, zagrożenie. Widać je na mapie w trybie „Oznaczenia drzew” i w eksporcie do QGIS.',
    inDemo: 'Przykłady: cmentarz (pomnik przyrody, kandydaci), aleja (zagrożenie, kolizja z infrastrukturą).',
  },
  'tree.valuation': {
    title: 'Waloryzacja',
    body: 'Zapis oceny arborysty: kategoria ogólna i opisy wartości (wiekowej, przyrodniczej, kulturowej…). Aplikacja nie liczy jej wzorem i nie wycenia drzewa.',
    inDemo: 'Kolory kategorii zobaczysz na mapie w trybie „Waloryzacja”.',
  },
  'tree.monitoring': {
    title: 'Monitoring stanu',
    body: 'Historia kontroli drzewa, od najnowszej: data, ocena (Dobry / Umiarkowany / Zły), uwagi i termin następnej kontroli.',
    inDemo: 'W alei lipowej drzewa mają kontrole z trzech sezonów.',
    more: 'Ocena „Zły” wysyła powiadomienie. Skala jest uproszczona (wersja 1) – to nie jest metoda VTA ani klasa ryzyka.',
  },
  'tree.mapSymbol': {
    title: 'Symbol na mapie',
    body: 'Kształt i kolor znaczka drzewa z katalogu symboli (koło, kwadrat, trójkąt, krzyż, romb, X). Znaczenie nadaje arborysta.',
  },
  'tree.photos': {
    title: 'Zdjęcia i adnotacje',
    body: 'Zdjęcia z aparatu tabletu lub z pliku. Na zdjęciu można narysować strzałki, linie i tekst – oryginał zostaje bez zmian.',
    inDemo: 'Otwórz zdjęcie w projekcie „Cmentarz parafialny” – wiele ma gotowe adnotacje. Zdjęcia w demo to ilustracje.',
    more: 'Zdjęcie zrobione bez zasięgu czeka na tablecie i wysyła się samo po połączeniu. Przy zdjęciu ustawia się podpis i to, czy trafia do dokumentu PDF.',
  },
  'tree.works': {
    title: 'Zalecenia i prace',
    body: 'Prace przy drzewie, np. cięcie sanitarne albo wycinka: status (Zalecenie → Zaplanowana → Wykonana), priorytet, termin i symbol na mapie.',
    inDemo: 'Prace z terminem trafiają do kalendarza (plik ICS) i przypomnień.',
  },
  'tree.deletion': {
    title: 'Usuwanie drzewa',
    body: 'Usuwa drzewo razem z jego danymi. Aplikacja pyta o potwierdzenie.',
    lockedInDemo: true,
  },
  'map.overview': {
    title: 'Mapa projektu',
    body: 'Wszystkie drzewa z lokalizacją na podkładzie mapowym. Kliknięcie drzewa pokazuje jego skrót i link do karty.',
    inDemo: 'Kliknij dowolny znaczek drzewa.',
  },
  'map.modes': {
    title: 'Tryby mapy',
    body: '„Drzewa i symbole prac”, „Oznaczenia drzew”, „Waloryzacja” (kolory kategorii) i „Gospodarka drzewostanem” (tylko prace do wykonania). Każdy tryb ma swoją legendę.',
  },
  'map.layers': {
    title: 'Warstwy i oznaczenia',
    body: 'Gwiazdka = drzewo iglaste, kółko = liściaste lub bez gatunku. Grupy drzew są osobną warstwą. Pod mapą wybierasz podkład (OpenStreetMap, ortofotomapa, mapa topograficzna) i działki ewidencyjne.',
    more: 'Podkłady Geoportalu wymagają internetu. Do projektu można też dodać warstwy referencyjne, np. z QGIS.',
  },
  'map.addTree': {
    title: 'Dodaj drzewo na mapie',
    body: 'Kliknięcie w miejsce drzewa od razu ustawia jego lokalizację i otwiera kartę nowego drzewa.',
    lockedInDemo: true,
  },
  'stands': {
    title: 'Grupy drzew',
    body: 'Obszar na mapie, np. aleja, szpaler albo zadrzewienie: rysowany na mapie albo importowany z pliku. Powierzchnia liczy się sama, a drzewa w obszarze można przypisać jednym kliknięciem.',
    inDemo: 'Park ma cztery grupy drzew – przełącz je na mapie.',
  },
  'management': {
    title: 'Gospodarka drzewostanem',
    body: 'Wszystkie prace projektu w jednym widoku z filtrami statusu i priorytetu oraz szybkim filtrem „najbliższe 7 dni”.',
    inDemo: 'Projekt „Teren szkoły” ma prace wykonane, zaplanowane i zalecane – terminy są liczone od dzisiaj.',
  },
  'documentation.report': {
    title: 'Dokument projektu (PDF)',
    body: 'Jednym kliknięciem: dane zlecenia, podsumowanie, zestawienie drzew, prace, grupy drzew i karty drzew ze zdjęciami.',
    inDemo: 'Możesz wygenerować i pobrać PDF z danymi przykładowymi.',
  },
  'documentation.materials': {
    title: 'Materiały do dokumentacji',
    body: 'Wszystkie zdjęcia projektu pogrupowane według drzew, w wersji z adnotacjami – z podpisami i informacją, które trafiają do PDF.',
  },
  'import.trees': {
    title: 'Import drzew',
    body: 'CSV, XLSX, GeoJSON, Shapefile, GeoPackage i DXF. Najpierw walidacja całego pliku z listą błędów – „wszystko albo nic”. Współrzędne PL-2000 i PL-1992 są przeliczane automatycznie.',
    lockedInDemo: true,
  },
  'import.stands': {
    title: 'Import grup drzew',
    body: 'Obszary z plików DXF, Shapefile, GeoJSON i GeoPackage – każdy obiekt tworzy nową grupę drzew.',
    lockedInDemo: true,
  },
  'export': {
    title: 'Eksport danych i QGIS',
    body: 'CSV, XLSX, GeoJSON, Shapefile, GeoPackage, pakiet dla QGIS (ze stylami) i kalendarz prac. GeoPackage i pakiet QGIS również w układach PL-2000 i PL-1992.',
    inDemo: 'Pobierz dowolny plik – eksport działa w demo.',
  },
  'integrations.calendar': {
    title: 'Kalendarz prac',
    body: 'Plik ICS z pracami z terminami – do zaimportowania w Google Calendar, Outlooku albo kalendarzu Apple.',
    inDemo: 'Możesz pobrać plik.',
  },
  'integrations.notifications': {
    title: 'Powiadomienia',
    body: 'Lista ostatnich powiadomień (np. drzewo w złym stanie, prace na jutro) i powiadomienia systemowe Web Push na danym urządzeniu.',
    inDemo: 'Lista jest widoczna; włączanie Web Push i powiadomienie testowe są w demo wyłączone.',
  },
  'workMode.switch': {
    title: 'Tryb terenowy',
    body: 'Widok pod tablet w terenie: mapa na cały ekran, bieżąca pozycja, duże przyciski i panel drzewa. Przełączenie zachowuje projekt i drzewo.',
    inDemo: 'Możesz wejść do trybu terenowego i przeglądać mapę oraz panele drzew.',
  },
  'field.projects': {
    title: 'Projekty w terenie',
    body: 'Duże wiersze wygodne na tablecie. Tworzenie projektów jest w widoku biurowym.',
  },
  'field.map': {
    title: 'Mapa terenowa',
    body: '„+ Drzewo tutaj” mierzy pozycję GPS i otwiera panel nowego drzewa. „Wskaż na mapie” – punkt ręcznie. „Moja pozycja” centruje mapę. „Warstwy” – tryb mapy, podkład i legenda.',
    inDemo: '„Moja pozycja” i „Warstwy” działają; dodawanie drzew jest wyłączone.',
  },
  'field.offline': {
    title: 'Praca bez zasięgu',
    body: '„Przygotuj projekt do pracy bez zasięgu” pobiera dane projektu na urządzenie. Zmiany zrobione offline czekają w kolejce i wysyłają się po powrocie sieci.',
    inDemo: 'Kolejka zmian i synchronizacja są w demo wyłączone, bo nic nie jest zapisywane.',
    more: 'Podkład mapy i zdjęcia nie są pobierane do pracy offline. Grupy drzew offline nie działają.',
  },
  'profile': {
    title: 'Mój profil',
    body: 'Dane konta i dane autora opracowania (imię i nazwisko, kwalifikacje, firma, opis metod), które trafiają do dokumentu PDF.',
    inDemo: 'Tylko podgląd – konto demo nie zmienia danych.',
  },
} satisfies Record<string, DemoHintContent>;

export type DemoHintId = keyof typeof DEMO_HINTS;

/** Komunikaty po kliknieciu zablokowanej funkcji (pokazywane zamiast otwarcia formularza). */
export const DEMO_BLOCKED_ACTION_MESSAGES = {
  NEW_PROJECT: 'Zakładanie projektów jest dostępne po zalogowaniu. W trybie demo możesz przeglądać cztery projekty przykładowe.',
  EDIT_PROJECT: 'Edycja danych zlecenia jest dostępna po zalogowaniu. W trybie demo dane projektu są tylko do podglądu.',
  ADD_TREE: 'Dodawanie drzew jest dostępne po zalogowaniu. W trybie demo możesz przeglądać przykładowe drzewa i ich dokumentację.',
  ADD_TREE_ON_MAP: 'Dodawanie drzew na mapie jest dostępne po zalogowaniu. W trybie demo kliknij istniejące drzewo, żeby zobaczyć jego dane.',
  DELETE_TREE: 'Usuwanie drzew jest dostępne po zalogowaniu. W trybie demo dane przykładowe pozostają bez zmian.',
  ADD_PHOTO: 'Dodawanie zdjęć jest dostępne po zalogowaniu. W trybie demo możesz oglądać zdjęcia i adnotacje przykładowych drzew.',
  EDIT_STAND: 'Tworzenie i edycja grup drzew są dostępne po zalogowaniu. W trybie demo możesz oglądać grupy na mapie.',
  IMPORT: 'Import plików jest dostępny po zalogowaniu, bo dodaje dane do projektu. W trybie demo możesz za to pobrać eksport.',
  EDIT_PROFILE: 'Edycja profilu i usuwanie konta są dostępne po zalogowaniu na własne konto.',
  FIELD_ADD_TREE: 'Dodawanie drzew w terenie jest dostępne po zalogowaniu. W trybie demo dotknij drzewa na mapie, żeby otworzyć jego panel.',
} as const;

export type DemoBlockedAction = keyof typeof DEMO_BLOCKED_ACTION_MESSAGES;

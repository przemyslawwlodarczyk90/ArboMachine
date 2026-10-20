// Moduly panelu arborysty w budowie (zakladki obok "Projekty"). Tresc stron "w budowie": co juz dziala w projektach,
// co planujemy i co trzeba ustalic z arborysta. Modul bez React - dane do nawigacji i strony PlannedModulePage.

export type PlannedModuleIcon = 'assessment' | 'report' | 'application' | 'calendar';

export interface PlannedModule {
  /** fragment adresu: /arborist/<slug> */
  slug: string;
  title: string;
  icon: PlannedModuleIcon;
  /** jedno zdanie pod tytulem */
  lead: string;
  /** funkcje, ktore juz sa w aplikacji (w projektach) i beda podstawa modulu */
  alreadyAvailable: string[];
  /** co modul ma robic */
  planned: string[];
  /** pytania do arborysty - pokazywane jako "Do doprecyzowania" */
  openQuestions: string[];
}

export const PLANNED_MODULES: PlannedModule[] = [
  {
    slug: 'oceny-drzew',
    title: 'Oceny drzew',
    icon: 'assessment',
    lead: 'Ocena wizualna drzew, stanu zdrowotnego i poziomu ryzyka - zebrana ze wszystkich projektów w jednym miejscu.',
    alreadyAvailable: [
      'Waloryzacja drzewa (ocena ogólna i wartości) w karcie drzewa',
      'Monitoring stanu - historia kontroli z oceną i uwagami',
      'Oznaczenia drzewa: zagrożenie, złom, posusz, owocniki grzybów',
      'Filtry listy drzew wg waloryzacji, stanu i oznaczeń',
    ],
    planned: [
      'Formularz oceny wizualnej (VTA) z listą objawów i uszkodzeń',
      'Ocena poziomu ryzyka z uzasadnieniem i zalecanym terminem ponownej kontroli',
      'Zestawienie ocen ze wszystkich projektów: drzewa ryzykowne na początku',
      'Przypomnienia o kolejnych kontrolach',
    ],
    openQuestions: [
      'Jaką metodę oceny stosuje arborysta (VTA, inna) i jakie pola musi mieć formularz?',
      'Czy poziom ryzyka ma mieć stałą skalę (np. niski / średni / wysoki), czy opis?',
      'Kto i kiedy wymaga ponownej kontroli (zleceniodawca, urząd)?',
    ],
  },
  {
    slug: 'raporty-z-prac',
    title: 'Raporty z prac',
    icon: 'report',
    lead: 'Dokumentowanie wykonanych cięć, wycinek i innych zabiegów - z protokołem dla zleceniodawcy.',
    alreadyAvailable: [
      'Zalecenia i prace przy drzewie: status, priorytet, terminy wykonania',
      'Widok „Gospodarka drzewostanem” z pracami do wykonania',
      'Zdjęcia drzewa z adnotacjami (rysowanie na zdjęciu)',
      'Dokument projektu PDF oraz eksport CSV / XLSX / GIS',
    ],
    planned: [
      'Protokół wykonania prac: data, zakres, zdjęcia przed i po',
      'Raport dla zleceniodawcy z wielu projektów i okresu',
      'Podpis / akceptacja protokołu przez zleceniodawcę',
      'Zestawienie godzin i kosztów prac',
    ],
    openQuestions: [
      'Jak dziś wygląda protokół z prac - czy jest wzór do odwzorowania?',
      'Czy raport ma zawierać koszty i czas pracy?',
      'Czy zleceniodawca ma akceptować protokół w aplikacji?',
    ],
  },
  {
    slug: 'wnioski',
    title: 'Wnioski',
    icon: 'application',
    lead: 'Przygotowanie wniosków do urzędu (np. o zezwolenie na usunięcie drzew) z danych zebranych w projekcie.',
    alreadyAvailable: [
      'Dane zlecenia projektu: zleceniodawca, adres, data',
      'Inwentaryzacja drzew z lokalizacją (WGS 84, PL-2000, PL-1992)',
      'Oznaczenie „do usunięcia” i prace z planowaną wycinką',
      'Mapa z podkładem Geoportalu i działkami ewidencyjnymi',
    ],
    planned: [
      'Wniosek wypełniany automatycznie danymi drzew przeznaczonych do usunięcia',
      'Załączniki: mapa z lokalizacją drzew, zdjęcia, inwentaryzacja',
      'Śledzenie statusu wniosku (złożony, uzupełnienie, decyzja)',
      'Przekazanie wniosku do panelu urzędnika',
    ],
    openQuestions: [
      'Jakie wnioski składa arborysta najczęściej i do jakich urzędów?',
      'Czy urzędy mają własne wzory formularzy (gmina, RDOŚ, konserwator zabytków)?',
      'Czy wniosek składa arborysta, czy zleceniodawca (właściciel terenu)?',
    ],
  },
  {
    slug: 'harmonogram',
    title: 'Harmonogram',
    icon: 'calendar',
    lead: 'Planowanie wizyt w terenie, terminów prac i kolejnych zleceń w jednym kalendarzu.',
    alreadyAvailable: [
      'Planowane terminy prac przy drzewach',
      'Eksport terminów do kalendarza (plik ICS - Google, Outlook)',
      'Powiadomienia push o pracach i kontrolach',
      'Mapa projektu z drzewami do odwiedzenia',
    ],
    planned: [
      'Kalendarz wszystkich projektów: wizyty, prace, kontrole',
      'Plan dnia w terenie z trasą na mapie',
      'Przypomnienia o zbliżających się terminach i zaległościach',
      'Synchronizacja z kalendarzem Google / Outlook',
    ],
    openQuestions: [
      'Czy arborysta pracuje sam, czy z ekipą (przydział zadań osobom)?',
      'Jakiego kalendarza używa dziś (Google, Outlook, papier)?',
      'Czy plan dnia ma uwzględniać kolejność dojazdu?',
    ],
  },
];

export const plannedModuleBySlug = (slug: string | undefined) => PLANNED_MODULES.find((module) => module.slug === slug) ?? null;

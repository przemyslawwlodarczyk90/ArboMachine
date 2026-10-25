// Etap 46/47: czy przejscie w aplikacji opuszcza biezacy ekran (i przy niezapisanych zmianach ma zapytac o decyzje).
// Modul bez React - testowany przez node --test.

export interface RouteLocation {
  pathname: string;
  search: string;
}

/**
 * Opuszcza ekran = inna sciezka. Sama zmiana zapytania (np. ?drzewo= w trybie terenowym) NIE - wybor innego drzewa
 * pilnuje juz runGuarded przed nawigacja (i czysci rejestr), wiec drugie pytanie byloby zbedne.
 */
export const isLeavingCurrentScreen = (current: RouteLocation, next: RouteLocation) =>
  current.pathname !== next.pathname;

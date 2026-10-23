// Etap 44: kiedy karta drzewa nie pozwala usunac drzewa. Modul bez React - testowany przez node --test.

/** Powod, dla ktorego drzewa nie mozna teraz usunac, albo null. Czysta funkcja - kolejnosc = pierwszy powod wygrywa. */
export function treeDeletionBlockReason({ isOnline, isCreatedOffline, hasPendingChanges }: {
  isOnline: boolean; isCreatedOffline: boolean; hasPendingChanges: boolean;
}): string | null {
  if (isCreatedOffline) return 'Drzewo nie trafiło jeszcze na serwer - usuniesz je po synchronizacji.';
  if (hasPendingChanges) return 'Drzewo ma zmiany czekające na wysłanie - usuniesz je po synchronizacji.';
  if (!isOnline) return 'Usuwanie drzewa wymaga połączenia z internetem.';
  return null;
}

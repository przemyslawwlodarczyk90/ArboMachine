// Proponowany numer nowego drzewa (etap 32, V1 do potwierdzenia z arborysta): najwiekszy numer czysto liczbowy
// w projekcie + 1. To tylko podpowiedz w polu, ktora mozna zmienic - nie autonumeracja i nie regula unikalnosci
// (backend przyjmuje dowolny tekst). Modul bez React - testowany przez node --test.

const PURELY_NUMERIC_PATTERN = /^\d+$/;

/** Pusty napis = brak propozycji (w projekcie nie ma numerow czysto liczbowych). */
export function suggestNextInventoryNumber(inventoryNumbers: string[]): string {
  let largest: bigint | null = null;
  for (const inventoryNumber of inventoryNumbers) {
    const trimmed = inventoryNumber.trim();
    if (!PURELY_NUMERIC_PATTERN.test(trimmed)) continue;
    const value = BigInt(trimmed);
    if (largest === null || value > largest) largest = value;
  }
  return largest === null ? '' : (largest + 1n).toString();
}

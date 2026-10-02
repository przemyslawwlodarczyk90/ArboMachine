import type { TreeImportFormat, TreeImportIssue } from './dataExchangeDataTypes';

// Polskie zdania dla kodow problemow importu (backend zwraca kod + wiersz + kolumne). Modul bez importow wartosci -
// testowany przez node --test. Nigdy nie pokazujemy nazw wyjatkow ani komunikatow technicznych.

const FILE_MESSAGES: Record<string, string> = {
  UNSUPPORTED_FILE_FORMAT: 'Nieobsługiwany format pliku. Wybierz plik CSV, XLSX, GeoJSON, Shapefile (ZIP), GeoPackage albo DXF.',
  FILE_TOO_LARGE: 'Plik jest za duży (maksymalnie 15 MB).',
  EMPTY_FILE: 'Plik jest pusty.',
  UNREADABLE_FILE: 'Nie udało się odczytać pliku. Sprawdź, czy to poprawny plik CSV / XLSX (format .xls nie jest obsługiwany).',
  INVALID_ENCODING: 'Plik CSV nie jest zapisany w kodowaniu UTF-8. Zapisz go ponownie jako „CSV UTF-8”.',
  INVALID_GEOJSON: 'Plik nie jest poprawnym plikiem GeoJSON (oczekiwano FeatureCollection albo Feature).',
  NO_DATA_ROWS: 'Plik nie zawiera żadnych drzew (jest tylko nagłówek).',
  TOO_MANY_ROWS: 'Plik ma za dużo obiektów (maksymalnie 2000 drzew albo 500 obszarów w jednym imporcie).',
  NO_POINT_FEATURES: 'Plik nie zawiera punktów drzew - same obszary zaimportujesz jako grupy drzew (niżej).',
  NO_SHAPEFILE_IN_ZIP: 'Archiwum ZIP nie zawiera pliku .shp z kompletem .dbf - spakuj wszystkie pliki warstwy (shp, shx, dbf, prj).',
  NO_POLYGON_FEATURES: 'Plik nie zawiera obszarów (wielokątów). Punkty drzew zaimportujesz w sekcji „Import drzew”.',
};

/** "Wiersz 17" w CSV/XLSX, "Obiekt 3" w GeoJSON; null dla problemow calego pliku. */
export function locationOfIssue(issue: TreeImportIssue, format: TreeImportFormat | null): string | null {
  if (issue.rowNumber === null) return null;
  return format === 'CSV' || format === 'XLSX' ? `Wiersz ${issue.rowNumber}` : `Obiekt ${issue.rowNumber}`;
}

const columnPart = (issue: TreeImportIssue) => (issue.column ? `„${issue.column}”` : 'wartość');
const valuePart = (issue: TreeImportIssue) => (issue.value ? ` („${issue.value}”)` : '');

/** Pelne zdanie po polsku, np. "Wiersz 42: nieprawidłowa liczba w kolumnie „Wysokość [m]” („dużo”)." */
export function describeImportIssue(issue: TreeImportIssue, format: TreeImportFormat | null): string {
  const prefix = locationOfIssue(issue, format);
  const sentence = (text: string) => (prefix ? `${prefix}: ${text}` : text.charAt(0).toUpperCase() + text.slice(1));
  switch (issue.code) {
    case 'MISSING_REQUIRED_COLUMN':
      return `Brak wymaganej kolumny „${issue.column ?? 'Numer inwentaryzacyjny'}”.`;
    case 'UNSUPPORTED_CRS':
      return `Nieobsługiwany układ współrzędnych${issue.column && issue.column !== '?' ? ` (${issue.column})` : ''}. Obsługiwane: WGS 84 (EPSG:4326), PL-2000 (strefy 5–8) i PL-1992.`;
    case 'REQUIRED_VALUE_MISSING':
      return sentence('brak numeru inwentaryzacyjnego.');
    case 'TEXT_TOO_LONG':
      return sentence(`za długi tekst w kolumnie ${columnPart(issue)}.`);
    case 'INVALID_NUMBER':
      return sentence(`nieprawidłowa liczba w kolumnie ${columnPart(issue)}${valuePart(issue)}.`);
    case 'NOT_A_WHOLE_NUMBER':
      return sentence(`w kolumnie ${columnPart(issue)} wymagana jest liczba całkowita${valuePart(issue)}.`);
    case 'TOO_MANY_DECIMAL_PLACES':
      return sentence(`w kolumnie ${columnPart(issue)} dopuszczalne są najwyżej 2 miejsca po przecinku${valuePart(issue)}.`);
    case 'VALUE_OUT_OF_RANGE':
      return sentence(`wartość spoza dopuszczalnego zakresu w kolumnie ${columnPart(issue)}${valuePart(issue)}.`);
    case 'INCOMPLETE_LOCATION':
      return sentence('lokalizacja wymaga obu współrzędnych - szerokości i długości geograficznej.');
    case 'UNSUPPORTED_GEOMETRY':
      return sentence(`nieobsługiwana geometria${valuePart(issue)} - drzewo musi być punktem (Point).`);
    case 'INVALID_POLYGON':
      return sentence('nieprawidłowy obszar - potrzeba co najmniej 3 różnych punktów i dodatniej powierzchni.');
    case 'COORDINATES_CONVERTED':
      return `Współrzędne przeliczono z układu ${issue.column ?? ''} do WGS 84.`;
    case 'NUMBERS_READ_FROM_DRAWING_LABELS':
      return 'Numery drzew odczytano z opisów na rysunku (dopasowanie odnośników) - po imporcie sprawdź numery. Pewniejszym źródłem jest arkusz XLSX/CSV z tej samej inwentaryzacji.';
    case 'AREA_ROW_SKIPPED':
      return sentence(`to obszar (powierzchnia ${issue.value ?? '?'} m²), a nie drzewo - pominięty. Obszary zaimportujesz z rysunku DXF jako grupy drzew.`);
    case 'MULTI_STEM_CIRCUMFERENCE_IN_NOTES':
      return sentence(`obwód wielopniowy („${issue.value ?? ''}”) zapisano w notatkach - pole obwodu zostaje puste (aplikacja nie przelicza obwodów wielopniowych).`);
    case 'DUPLICATE_INVENTORY_NUMBER_IN_FILE':
      return sentence(`numer „${issue.value ?? ''}” powtarza się w pliku - zostanie utworzone kolejne drzewo.`);
    case 'INVENTORY_NUMBER_EXISTS_IN_PROJECT':
      return sentence(`drzewo o numerze „${issue.value ?? ''}” już jest w projekcie - import utworzy nowe drzewo (nie zaktualizuje istniejącego).`);
    default:
      return FILE_MESSAGES[issue.code] ?? sentence('nieprawidłowe dane.');
  }
}

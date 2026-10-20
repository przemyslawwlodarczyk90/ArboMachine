// Warstwy referencyjne (etap 31): kolory warstw i komunikaty odrzuconych plikow po polsku.
// Modul bez React - testowany przez node --test.

/** Kolory kolejnych warstw - stonowane, czytelne na OSM i ortofotomapie, rozne od zieleni drzew. */
export const REFERENCE_LAYER_COLORS = ['#1971c2', '#c2255c', '#e67700', '#6741d9', '#0c8599', '#5c940d', '#495057'];

export const referenceLayerColorOf = (index: number) => REFERENCE_LAYER_COLORS[index % REFERENCE_LAYER_COLORS.length];

/** Kod odrzucenia z backendu (400 { code, detail }) -> komunikat dla arborysty. */
export function describeRejectedLayerFile(code: string | undefined, detail?: string): string {
  switch (code) {
    case 'UNSUPPORTED_FILE_FORMAT':
      return 'Nieobsługiwany format pliku. Wybierz GeoJSON (.geojson), Shapefile w ZIP (.zip), GeoPackage (.gpkg) albo DXF.';
    case 'EMPTY_FILE':
      return 'Plik jest pusty.';
    case 'FILE_TOO_LARGE':
      return 'Plik jest za duży (limit 15 MB). Zostaw w QGIS tylko potrzebny obszar i warstwy.';
    case 'UNREADABLE_FILE':
      return 'Nie udało się odczytać pliku - sprawdź, czy nie jest uszkodzony (Shapefile: spakuj .shp, .shx, .dbf i .prj do ZIP).';
    case 'UNSUPPORTED_CRS':
      return `Nieobsługiwany układ współrzędnych${detail && detail !== '?' ? ` (${detail})` : ''}. Zapisz warstwę w QGIS w WGS 84, PL-2000 albo PL-1992.`;
    case 'INVALID_COORDINATES':
      return 'Współrzędne poza zakresem - plik prawdopodobnie nie opisuje układu (np. GeoJSON w metrach PL-2000). Zapisz go w QGIS z układem EPSG.';
    case 'NO_FEATURES':
      return 'W pliku nie ma obiektów z geometrią (punkty, linie, obszary).';
    case 'TOO_MANY_FEATURES':
      return 'Warstwa ma zbyt wiele obiektów do pokazania na tablecie. Przytnij ją w QGIS do obszaru projektu.';
    case 'TOO_MANY_LAYERS':
      return 'Projekt ma już maksymalną liczbę warstw referencyjnych (20). Usuń niepotrzebną warstwę.';
    default:
      return 'Nie udało się dodać warstwy.';
  }
}

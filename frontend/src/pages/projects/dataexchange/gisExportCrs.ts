// Etap 34: uklad wspolrzednych eksportu do QGIS (GeoPackage i pakiet QGIS). Domena trzyma WGS 84 - przeliczenie robi
// backend przy zapisie pliku (parametr crs). Modul bez React - testowany przez node --test.

export type GisExportCrs = 'PL-2000' | 'EPSG:2176' | 'EPSG:2177' | 'EPSG:2178' | 'EPSG:2179' | 'EPSG:2180' | 'EPSG:4326';

export const GIS_EXPORT_CRS_OPTIONS: { crs: GisExportCrs; label: string }[] = [
  { crs: 'PL-2000', label: 'PL-2000 - strefa dobrana do drzew projektu' },
  { crs: 'EPSG:2176', label: 'PL-2000 strefa 5 (EPSG:2176)' },
  { crs: 'EPSG:2177', label: 'PL-2000 strefa 6 (EPSG:2177)' },
  { crs: 'EPSG:2178', label: 'PL-2000 strefa 7 (EPSG:2178)' },
  { crs: 'EPSG:2179', label: 'PL-2000 strefa 8 (EPSG:2179)' },
  { crs: 'EPSG:2180', label: 'PL-1992 (EPSG:2180)' },
  { crs: 'EPSG:4326', label: 'WGS 84 - stopnie (EPSG:4326)' },
];

/** V1 (do potwierdzenia, 02 poz. 9.2): domyslnie PL-2000 - uklad dokumentow geodezyjnych i urzedow w Polsce. */
export const DEFAULT_GIS_EXPORT_CRS: GisExportCrs = 'PL-2000';

/** Formaty eksportu, w ktorych uzytkownik wybiera uklad (pozostale GIS sa w WGS 84). */
export const CRS_AWARE_EXPORT_FILES = ['gpkg', 'qgis-zip'] as const;

/** Zapytanie adresu pliku eksportu: "?crs=..." tylko dla GeoPackage i pakietu QGIS. */
export function exportQueryOf(exportFile: string, crs: GisExportCrs | null): string {
  if (!crs || !(CRS_AWARE_EXPORT_FILES as readonly string[]).includes(exportFile)) return '';
  return `?crs=${encodeURIComponent(crs)}`;
}

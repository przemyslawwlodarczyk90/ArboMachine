// Typy importu/eksportu drzew - odpowiadaja DTO z pakietu domain.dataexchange (TreeImportValidationResponse itd.).

export type TreeImportIssueCode =
  | 'UNSUPPORTED_FILE_FORMAT' | 'FILE_TOO_LARGE' | 'EMPTY_FILE' | 'UNREADABLE_FILE' | 'INVALID_ENCODING'
  | 'INVALID_GEOJSON' | 'UNSUPPORTED_CRS' | 'MISSING_REQUIRED_COLUMN' | 'NO_DATA_ROWS' | 'TOO_MANY_ROWS'
  | 'NO_POINT_FEATURES' | 'NO_SHAPEFILE_IN_ZIP' | 'NO_POLYGON_FEATURES'
  | 'REQUIRED_VALUE_MISSING' | 'TEXT_TOO_LONG' | 'INVALID_NUMBER' | 'NOT_A_WHOLE_NUMBER' | 'TOO_MANY_DECIMAL_PLACES'
  | 'VALUE_OUT_OF_RANGE' | 'INCOMPLETE_LOCATION' | 'UNSUPPORTED_GEOMETRY' | 'INVALID_POLYGON'
  | 'DUPLICATE_INVENTORY_NUMBER_IN_FILE' | 'INVENTORY_NUMBER_EXISTS_IN_PROJECT'
  | 'COORDINATES_CONVERTED' | 'NUMBERS_READ_FROM_DRAWING_LABELS' | 'AREA_ROW_SKIPPED' | 'MULTI_STEM_CIRCUMFERENCE_IN_NOTES';

/** Problem importu: rowNumber null = caly plik; column = naglowek z pliku uzytkownika; field = nazwa techniczna. */
export interface TreeImportIssue {
  rowNumber: number | null;
  field: string | null;
  column: string | null;
  value: string | null;
  code: TreeImportIssueCode;
}

export type TreeImportFormat = 'CSV' | 'XLSX' | 'GEOJSON' | 'SHAPEFILE' | 'GEOPACKAGE' | 'DXF';

export interface TreeImportPreviewRow {
  rowNumber: number;
  inventoryNumber: string;
  speciesPolish: string | null;
  speciesLatin: string | null;
  trunkCircumferenceCm: number | null;
  heightM: number | null;
  crownDiameterM: number | null;
  hasLocation: boolean;
}

export interface TreeImportValidation {
  fileName: string;
  format: TreeImportFormat | null;
  totalRowCount: number;
  validRowCount: number;
  invalidRowCount: number;
  readyToImport: boolean;
  fileErrors: TreeImportIssue[];
  errors: TreeImportIssue[];
  warnings: TreeImportIssue[];
  ignoredColumns: string[];
  preview: TreeImportPreviewRow[];
}

export interface TreeImportResult {
  importedTreeCount: number;
  warningCount: number;
}

export type TreeExportFormat = 'csv' | 'xlsx' | 'geojson' | 'gpkg';

/** Pliki eksportu projektu: drzewa w formatach jak wyzej + (etap 28) CSV prac - jeden wiersz na prace. */
export type ProjectExportFile = TreeExportFormat | 'works-csv' | 'calendar-ics' | 'shapefile-zip' | 'stands-geojson'
  /** etap 34: pakiet dla QGIS - GeoPackage ze stylami + pliki QML + README */
  | 'qgis-zip';

/** Etap 29: wynik "Sprawdz plik" importu grup drzew (obszarow). */
export interface TreeStandImportValidation {
  fileName: string;
  format: TreeImportFormat | null;
  featureCount: number;
  validStandCount: number;
  invalidFeatureCount: number;
  readyToImport: boolean;
  fileErrors: TreeImportIssue[];
  errors: TreeImportIssue[];
  warnings: TreeImportIssue[];
  preview: { rowNumber: number; name: string; areaSquareMeters: number | null; vertexCount: number; treeCountInside: number }[];
}

export interface TreeStandImportResult {
  importedStandCount: number;
  assignedTreeCount: number;
}

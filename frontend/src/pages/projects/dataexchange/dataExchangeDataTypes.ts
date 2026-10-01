// Typy importu/eksportu drzew - odpowiadaja DTO z pakietu domain.dataexchange (TreeImportValidationResponse itd.).

export type TreeImportIssueCode =
  | 'UNSUPPORTED_FILE_FORMAT' | 'FILE_TOO_LARGE' | 'EMPTY_FILE' | 'UNREADABLE_FILE' | 'INVALID_ENCODING'
  | 'INVALID_GEOJSON' | 'UNSUPPORTED_CRS' | 'MISSING_REQUIRED_COLUMN' | 'NO_DATA_ROWS' | 'TOO_MANY_ROWS'
  | 'REQUIRED_VALUE_MISSING' | 'TEXT_TOO_LONG' | 'INVALID_NUMBER' | 'NOT_A_WHOLE_NUMBER' | 'TOO_MANY_DECIMAL_PLACES'
  | 'VALUE_OUT_OF_RANGE' | 'INCOMPLETE_LOCATION' | 'UNSUPPORTED_GEOMETRY'
  | 'DUPLICATE_INVENTORY_NUMBER_IN_FILE' | 'INVENTORY_NUMBER_EXISTS_IN_PROJECT';

/** Problem importu: rowNumber null = caly plik; column = naglowek z pliku uzytkownika; field = nazwa techniczna. */
export interface TreeImportIssue {
  rowNumber: number | null;
  field: string | null;
  column: string | null;
  value: string | null;
  code: TreeImportIssueCode;
}

export type TreeImportFormat = 'CSV' | 'XLSX' | 'GEOJSON';

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

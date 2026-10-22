import { sendApiRequest, sendApiRequestForBlob } from '../../../api/httpClient';
import type {
  ProjectExportFile, TreeImportResult, TreeImportValidation, TreeStandImportResult, TreeStandImportValidation,
} from './dataExchangeDataTypes';
import { exportQueryOf, type GisExportCrs } from './gisExportCrs';

const projectPath = (projectId: number) => `/api/projects/${projectId}`;

const fileFormData = (file: File) => {
  const formData = new FormData();
  formData.append('file', file, file.name);
  return formData;
};

/** Import i eksport drzew projektu (backend: domain.dataexchange). Wlasnosc projektu sprawdza backend (404). */
export const dataExchangeApi = {
  /** Etap 34: crs - uklad GeoPackage i pakietu QGIS (inne formaty go ignoruja). */
  downloadProjectExport: (projectId: number, exportFile: ProjectExportFile, crs: GisExportCrs | null = null) =>
    sendApiRequestForBlob(`${projectPath(projectId)}/exports/${exportPathOf(exportFile)}${exportQueryOf(exportFile, crs)}`,
      { sendAccessToken: true }),

  /** "Sprawdz plik" - nic nie zapisuje. */
  validateTreeImport: (projectId: number, file: File) =>
    sendApiRequest<TreeImportValidation>(`${projectPath(projectId)}/imports/trees/validate`, {
      httpMethod: 'POST', requestBody: fileFormData(file), sendAccessToken: true,
    }),

  /** Etap 29: "Sprawdz plik" obszarow grup drzew (DXF, Shapefile ZIP, GeoJSON, GeoPackage) - nic nie zapisuje. */
  validateStandImport: (projectId: number, file: File) =>
    sendApiRequest<TreeStandImportValidation>(`${projectPath(projectId)}/imports/stands/validate`, {
      httpMethod: 'POST', requestBody: fileFormData(file), sendAccessToken: true,
    }),

  importStands: (projectId: number, file: File) =>
    sendApiRequest<TreeStandImportResult>(`${projectPath(projectId)}/imports/stands`, {
      httpMethod: 'POST', requestBody: fileFormData(file), sendAccessToken: true,
    }),

  /** Import "wszystko albo nic" - backend sprawdza plik ponownie; bledy -> 400. */
  importTrees: (projectId: number, file: File) =>
    sendApiRequest<TreeImportResult>(`${projectPath(projectId)}/imports/trees`, {
      httpMethod: 'POST', requestBody: fileFormData(file), sendAccessToken: true,
    }),
};

/** Nazwa pobieranego pliku - jak w Content-Disposition backendu (tylko id projektu). */
const exportPathOf = (exportFile: ProjectExportFile) => {
  if (exportFile === 'works-csv') return 'works.csv';
  if (exportFile === 'calendar-ics') return 'calendar.ics';
  if (exportFile === 'shapefile-zip') return 'shapefile.zip';
  if (exportFile === 'stands-geojson') return 'stands.geojson';
  if (exportFile === 'qgis-zip') return 'qgis.zip';
  return `trees.${exportFile}`;
};

/** Nazwy jak w Content-Disposition backendu (ExportedFile). */
export const projectExportFileName = (projectId: number, exportFile: ProjectExportFile) => {
  if (exportFile === 'works-csv') return `arbomachine-projekt-${projectId}-prace.csv`;
  if (exportFile === 'calendar-ics') return `arbomachine-projekt-${projectId}-kalendarz-prac.ics`;
  if (exportFile === 'shapefile-zip') return `arbomachine-projekt-${projectId}-shapefile.zip`;
  if (exportFile === 'stands-geojson') return `arbomachine-projekt-${projectId}-grupy-drzew.geojson`;
  if (exportFile === 'qgis-zip') return `arbomachine-projekt-${projectId}-qgis.zip`;
  return `arbomachine-projekt-${projectId}-drzewa.${exportFile}`;
};

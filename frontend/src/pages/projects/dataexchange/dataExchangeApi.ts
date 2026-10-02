import { sendApiRequest, sendApiRequestForBlob } from '../../../api/httpClient';
import type { ProjectExportFile, TreeImportResult, TreeImportValidation } from './dataExchangeDataTypes';

const projectPath = (projectId: number) => `/api/projects/${projectId}`;

const fileFormData = (file: File) => {
  const formData = new FormData();
  formData.append('file', file, file.name);
  return formData;
};

/** Import i eksport drzew projektu (backend: domain.dataexchange). Wlasnosc projektu sprawdza backend (404). */
export const dataExchangeApi = {
  downloadProjectExport: (projectId: number, exportFile: ProjectExportFile) =>
    sendApiRequestForBlob(`${projectPath(projectId)}/exports/${exportPathOf(exportFile)}`, { sendAccessToken: true }),

  /** "Sprawdz plik" - nic nie zapisuje. */
  validateTreeImport: (projectId: number, file: File) =>
    sendApiRequest<TreeImportValidation>(`${projectPath(projectId)}/imports/trees/validate`, {
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
  return `trees.${exportFile}`;
};

/** Nazwy jak w Content-Disposition backendu (ExportedFile). */
export const projectExportFileName = (projectId: number, exportFile: ProjectExportFile) => {
  if (exportFile === 'works-csv') return `arbomachine-projekt-${projectId}-prace.csv`;
  if (exportFile === 'calendar-ics') return `arbomachine-projekt-${projectId}-kalendarz-prac.ics`;
  return `arbomachine-projekt-${projectId}-drzewa.${exportFile}`;
};

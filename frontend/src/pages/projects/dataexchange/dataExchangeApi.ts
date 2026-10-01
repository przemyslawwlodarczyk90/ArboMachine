import { sendApiRequest, sendApiRequestForBlob } from '../../../api/httpClient';
import type { TreeExportFormat, TreeImportResult, TreeImportValidation } from './dataExchangeDataTypes';

const projectPath = (projectId: number) => `/api/projects/${projectId}`;

const fileFormData = (file: File) => {
  const formData = new FormData();
  formData.append('file', file, file.name);
  return formData;
};

/** Import i eksport drzew projektu (backend: domain.dataexchange). Wlasnosc projektu sprawdza backend (404). */
export const dataExchangeApi = {
  downloadTreesExport: (projectId: number, format: TreeExportFormat) =>
    sendApiRequestForBlob(`${projectPath(projectId)}/exports/trees.${format}`, { sendAccessToken: true }),

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
export const treesExportFileName = (projectId: number, format: TreeExportFormat) =>
  `arbomachine-projekt-${projectId}-drzewa.${format}`;

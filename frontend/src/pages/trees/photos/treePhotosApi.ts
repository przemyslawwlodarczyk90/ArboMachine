import { sendApiRequest, sendApiRequestForBlob } from '../../../api/httpClient';
import type { TreePhoto } from './treePhotoDataTypes';

const treePhotosPath = (projectId: number, treeId: number) => `/api/projects/${projectId}/trees/${treeId}/photos`;

/**
 * Endpointy zdjec drzewa (/api/projects/{projectId}/trees/{treeId}/photos/**). Dostep wynika z wlasnosci projektu,
 * ktora sprawdza backend (cala hierarchia projekt -> drzewo -> zdjecie) - frontend niczego tu nie filtruje.
 */
export const treePhotosApi = {
  listTreePhotos: (projectId: number, treeId: number) =>
    sendApiRequest<TreePhoto[]>(treePhotosPath(projectId, treeId), { sendAccessToken: true }),

  /** multipart/form-data z polem "file"; typ i rozmiar waliduje backend (zrodlo prawdy). */
  uploadTreePhoto: (projectId: number, treeId: number, photoFile: File) => {
    const uploadFormData = new FormData();
    uploadFormData.append('file', photoFile);
    return sendApiRequest<TreePhoto>(treePhotosPath(projectId, treeId), {
      httpMethod: 'POST', requestBody: uploadFormData, sendAccessToken: true,
    });
  },

  getTreePhotoContent: (projectId: number, treeId: number, photoId: number) =>
    sendApiRequestForBlob(`${treePhotosPath(projectId, treeId)}/${photoId}/content`, { sendAccessToken: true }),

  deleteTreePhoto: (projectId: number, treeId: number, photoId: number) =>
    sendApiRequest<void>(`${treePhotosPath(projectId, treeId)}/${photoId}`, {
      httpMethod: 'DELETE', sendAccessToken: true,
    }),
};

import { sendApiRequest, sendApiRequestForBlob } from '../../../api/httpClient';
import type { AnnotationDocument, PhotoAnnotation } from './annotations/annotationDataTypes';
import type { ProjectPhoto, TreePhoto } from './treePhotoDataTypes';

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

  /** Zdjecie do dokumentacji: wersja z adnotacja, jesli istnieje, w przeciwnym razie oryginal. */
  getTreePhotoDocumentationContent: (projectId: number, treeId: number, photoId: number) =>
    sendApiRequestForBlob(`${treePhotosPath(projectId, treeId)}/${photoId}/documentation-content`, { sendAccessToken: true }),

  /** Zdjecia wszystkich drzew projektu (materialy do dokumentacji) jednym zapytaniem. */
  listProjectPhotos: (projectId: number) =>
    sendApiRequest<ProjectPhoto[]>(`/api/projects/${projectId}/photos`, { sendAccessToken: true }),

  getTreePhotoAnnotation: (projectId: number, treeId: number, photoId: number) =>
    sendApiRequest<PhotoAnnotation>(`${treePhotosPath(projectId, treeId)}/${photoId}/annotation`, { sendAccessToken: true }),

  /** multipart: "document" (JSON) + "renderedImage" (splaszczony PNG); tworzy albo zastepuje adnotacje. */
  saveTreePhotoAnnotation: (projectId: number, treeId: number, photoId: number, document: AnnotationDocument,
                            renderedImage: Blob) => {
    const annotationFormData = new FormData();
    annotationFormData.append('document', new Blob([JSON.stringify(document)], { type: 'application/json' }));
    annotationFormData.append('renderedImage', renderedImage, 'adnotacja.png');
    return sendApiRequest<PhotoAnnotation>(`${treePhotosPath(projectId, treeId)}/${photoId}/annotation`, {
      httpMethod: 'PUT', requestBody: annotationFormData, sendAccessToken: true,
    });
  },

  deleteTreePhotoAnnotation: (projectId: number, treeId: number, photoId: number) =>
    sendApiRequest<void>(`${treePhotosPath(projectId, treeId)}/${photoId}/annotation`, {
      httpMethod: 'DELETE', sendAccessToken: true,
    }),

  deleteTreePhoto: (projectId: number, treeId: number, photoId: number) =>
    sendApiRequest<void>(`${treePhotosPath(projectId, treeId)}/${photoId}`, {
      httpMethod: 'DELETE', sendAccessToken: true,
    }),
};

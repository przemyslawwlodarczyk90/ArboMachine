import { sendApiRequest } from '../../../api/httpClient';
import type { ReferenceLayer } from './referenceLayerDataTypes';

const layersPath = (projectId: number) => `/api/projects/${projectId}/reference-layers`;

/** Warstwy referencyjne projektu (etap 31). Wymagaja polaczenia - wgrywanie i usuwanie nie ida do kolejki offline. */
export const referenceLayersApi = {
  listLayers: (projectId: number) => sendApiRequest<ReferenceLayer[]>(layersPath(projectId), { sendAccessToken: true }),

  getLayerGeoJson: (projectId: number, layerId: number) =>
    sendApiRequest<unknown>(`${layersPath(projectId)}/${layerId}/geojson`, { sendAccessToken: true }),

  uploadLayer: (projectId: number, file: File, name: string) => {
    const formData = new FormData();
    formData.append('file', file, file.name);
    if (name.trim()) formData.append('name', name.trim());
    return sendApiRequest<ReferenceLayer>(layersPath(projectId), { httpMethod: 'POST', requestBody: formData, sendAccessToken: true });
  },

  deleteLayer: (projectId: number, layerId: number) =>
    sendApiRequest<void>(`${layersPath(projectId)}/${layerId}`, { httpMethod: 'DELETE', sendAccessToken: true }),
};

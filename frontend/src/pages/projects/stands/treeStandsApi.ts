import { sendApiRequest } from '../../../api/httpClient';
import type { TreeStand, TreeStandRequest } from './standDataTypes';

const standsPath = (projectId: number) => `/api/projects/${projectId}/stands`;

/**
 * Grupy drzew projektu (etap 29). Wlasnosc projektu sprawdza backend (404). V1: tylko z polaczeniem - rysowanie
 * obszaru wymaga podkladu mapy (praca offline grup drzew - DO_DOPRECYZOWANIA). Lista jest kopiowana do pamieci
 * urzadzenia jak inne odczyty (podglad offline).
 */
export const treeStandsApi = {
  listStands: (projectId: number) => sendApiRequest<TreeStand[]>(standsPath(projectId), { sendAccessToken: true }),

  createStand: (projectId: number, request: TreeStandRequest) =>
    sendApiRequest<TreeStand>(standsPath(projectId), { httpMethod: 'POST', requestBody: request, sendAccessToken: true }),

  updateStand: (projectId: number, standId: number, request: TreeStandRequest) =>
    sendApiRequest<TreeStand>(`${standsPath(projectId)}/${standId}`, { httpMethod: 'PUT', requestBody: request, sendAccessToken: true }),

  deleteStand: (projectId: number, standId: number) =>
    sendApiRequest<void>(`${standsPath(projectId)}/${standId}`, { httpMethod: 'DELETE', sendAccessToken: true }),

  assignTreesInside: (projectId: number, standId: number) =>
    sendApiRequest<TreeStand>(`${standsPath(projectId)}/${standId}/assign-trees-inside`, { httpMethod: 'POST', sendAccessToken: true }),
};

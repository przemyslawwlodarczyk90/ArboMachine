import { sendApiRequest } from '../../../api/httpClient';
import type { TreeWork, TreeWorkFieldsRequest } from './treeWorkDataTypes';

const treeWorksPath = (projectId: number, treeId: number) => `/api/projects/${projectId}/trees/${treeId}/works`;

/**
 * Endpointy zalecen/prac przy drzewie (/api/projects/{projectId}/trees/{treeId}/works/**). Cala hierarchie
 * (wlasny projekt -> drzewo -> praca) sprawdza backend.
 */
export const treeWorksApi = {
  listTreeWorks: (projectId: number, treeId: number) =>
    sendApiRequest<TreeWork[]>(treeWorksPath(projectId, treeId), { sendAccessToken: true }),

  createTreeWork: (projectId: number, treeId: number, createTreeWorkRequest: TreeWorkFieldsRequest) =>
    sendApiRequest<TreeWork>(treeWorksPath(projectId, treeId), {
      httpMethod: 'POST', requestBody: createTreeWorkRequest, sendAccessToken: true,
    }),

  updateTreeWork: (projectId: number, treeId: number, workId: number, updateTreeWorkRequest: TreeWorkFieldsRequest) =>
    sendApiRequest<TreeWork>(`${treeWorksPath(projectId, treeId)}/${workId}`, {
      httpMethod: 'PATCH', requestBody: updateTreeWorkRequest, sendAccessToken: true,
    }),

  /** Prace wszystkich drzew projektu jednym zapytaniem (mapa projektu: symbole prac i legenda). */
  listProjectWorks: (projectId: number) =>
    sendApiRequest<TreeWork[]>(`/api/projects/${projectId}/works`, { sendAccessToken: true }),

  /** Ustawia albo zmienia symbol pracy na mapie - bez edycji nazwy i opisu. */
  setTreeWorkMapSymbol: (projectId: number, treeId: number, workId: number, symbolCode: string) =>
    sendApiRequest<TreeWork>(`${treeWorksPath(projectId, treeId)}/${workId}/map-symbol`, {
      httpMethod: 'PUT', requestBody: { symbolCode }, sendAccessToken: true,
    }),

  clearTreeWorkMapSymbol: (projectId: number, treeId: number, workId: number) =>
    sendApiRequest<TreeWork>(`${treeWorksPath(projectId, treeId)}/${workId}/map-symbol`, {
      httpMethod: 'DELETE', sendAccessToken: true,
    }),

  deleteTreeWork: (projectId: number, treeId: number, workId: number) =>
    sendApiRequest<void>(`${treeWorksPath(projectId, treeId)}/${workId}`, {
      httpMethod: 'DELETE', sendAccessToken: true,
    }),
};

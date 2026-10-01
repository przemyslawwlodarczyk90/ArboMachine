import { sendApiRequest } from '../../../api/httpClient';
import { offlineService } from '../../../offline/offlineService';
import type { TreeWork, TreeWorkFieldsRequest } from './treeWorkDataTypes';

const treeWorksPath = (projectId: number, treeId: number) => `/api/projects/${projectId}/trees/${treeId}/works`;

/**
 * Endpointy zalecen/prac przy drzewie (/api/projects/{projectId}/trees/{treeId}/works/**). Cala hierarchie
 * (wlasny projekt -> drzewo -> praca) sprawdza backend. Dodanie i edycja pracy dzialaja takze bez polaczenia
 * (offlineService, etap 26); symbole i usuwanie wymagaja polaczenia.
 */
export const treeWorksApi = {
  listTreeWorks: (projectId: number, treeId: number) =>
    sendApiRequest<TreeWork[]>(treeWorksPath(projectId, treeId), { sendAccessToken: true }),

  createTreeWork: (projectId: number, treeId: number, createTreeWorkRequest: TreeWorkFieldsRequest) =>
    offlineService.mutate<TreeWork>({ kind: 'CREATE_TREE_WORK', projectId, treeId, body: createTreeWorkRequest }),

  updateTreeWork: (projectId: number, treeId: number, workId: number, updateTreeWorkRequest: TreeWorkFieldsRequest) =>
    offlineService.mutate<TreeWork>({ kind: 'UPDATE_TREE_WORK', projectId, treeId, workId, body: updateTreeWorkRequest }),

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

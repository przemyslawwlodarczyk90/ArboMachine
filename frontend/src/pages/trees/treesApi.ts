import { sendApiRequest } from '../../api/httpClient';
import { offlineService } from '../../offline/offlineService';
import type { CreateTreeRequest, Tree, TreeLocationRequest, UpdateTreeRequest } from './treeDataTypes';

/**
 * Endpointy modulu drzew (/api/projects/{projectId}/trees/**). Dostep do drzew wynika z wlasnosci projektu,
 * ktora sprawdza backend - frontend niczego tu nie filtruje. Dodanie i edycja drzewa oraz lokalizacja dzialaja
 * takze bez polaczenia (offlineService: zapis na urzadzeniu i synchronizacja pozniej - etap 26); odczyty bez
 * polaczenia zwracaja ostatnia kopie z urzadzenia (httpClient).
 */
export const treesApi = {
  listTreesOfProject: (projectId: number) =>
    sendApiRequest<Tree[]>(`/api/projects/${projectId}/trees`, { sendAccessToken: true }),

  createTreeInProject: (projectId: number, createTreeRequest: CreateTreeRequest) =>
    offlineService.mutate<Tree>({ kind: 'CREATE_TREE', projectId, body: createTreeRequest }),

  getTreeOfProject: (projectId: number, treeId: number) =>
    sendApiRequest<Tree>(`/api/projects/${projectId}/trees/${treeId}`, { sendAccessToken: true }),

  updateTreeOfProject: (projectId: number, treeId: number, updateTreeRequest: UpdateTreeRequest) =>
    offlineService.mutate<Tree>({ kind: 'UPDATE_TREE', projectId, treeId, body: updateTreeRequest }),

  /** Ustawia albo zastepuje lokalizacje; zwraca zaktualizowane drzewo. */
  setTreeLocation: (projectId: number, treeId: number, treeLocationRequest: TreeLocationRequest) =>
    offlineService.mutate<Tree>({ kind: 'SET_TREE_LOCATION', projectId, treeId, body: treeLocationRequest }),

  /** Ustawia albo zmienia symbol drzewa na mapie; zwraca zaktualizowane drzewo. */
  setTreeMapSymbol: (projectId: number, treeId: number, symbolCode: string) =>
    sendApiRequest<Tree>(`/api/projects/${projectId}/trees/${treeId}/map-symbol`, {
      httpMethod: 'PUT', requestBody: { symbolCode }, sendAccessToken: true,
    }),

  /** Usuwa symbol drzewa (neutralny marker); zwraca zaktualizowane drzewo. */
  clearTreeMapSymbol: (projectId: number, treeId: number) =>
    sendApiRequest<Tree>(`/api/projects/${projectId}/trees/${treeId}/map-symbol`, {
      httpMethod: 'DELETE', sendAccessToken: true,
    }),

  /** Usuwa lokalizacje (drzewo wraca do stanu "nie ustalono"); zwraca zaktualizowane drzewo. */
  clearTreeLocation: (projectId: number, treeId: number) =>
    offlineService.mutate<Tree>({ kind: 'CLEAR_TREE_LOCATION', projectId, treeId }),
};

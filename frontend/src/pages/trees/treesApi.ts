import { sendApiRequest } from '../../api/httpClient';
import type { CreateTreeRequest, Tree, TreeLocationRequest, UpdateTreeRequest } from './treeDataTypes';

/**
 * Endpointy modulu drzew (/api/projects/{projectId}/trees/**). Dostep do drzew wynika z wlasnosci projektu,
 * ktora sprawdza backend - frontend niczego tu nie filtruje.
 */
export const treesApi = {
  listTreesOfProject: (projectId: number) =>
    sendApiRequest<Tree[]>(`/api/projects/${projectId}/trees`, { sendAccessToken: true }),

  createTreeInProject: (projectId: number, createTreeRequest: CreateTreeRequest) =>
    sendApiRequest<Tree>(`/api/projects/${projectId}/trees`, {
      httpMethod: 'POST', requestBody: createTreeRequest, sendAccessToken: true,
    }),

  getTreeOfProject: (projectId: number, treeId: number) =>
    sendApiRequest<Tree>(`/api/projects/${projectId}/trees/${treeId}`, { sendAccessToken: true }),

  updateTreeOfProject: (projectId: number, treeId: number, updateTreeRequest: UpdateTreeRequest) =>
    sendApiRequest<Tree>(`/api/projects/${projectId}/trees/${treeId}`, {
      httpMethod: 'PATCH', requestBody: updateTreeRequest, sendAccessToken: true,
    }),

  /** Ustawia albo zastepuje lokalizacje; zwraca zaktualizowane drzewo. */
  setTreeLocation: (projectId: number, treeId: number, treeLocationRequest: TreeLocationRequest) =>
    sendApiRequest<Tree>(`/api/projects/${projectId}/trees/${treeId}/location`, {
      httpMethod: 'PUT', requestBody: treeLocationRequest, sendAccessToken: true,
    }),

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
    sendApiRequest<Tree>(`/api/projects/${projectId}/trees/${treeId}/location`, {
      httpMethod: 'DELETE', sendAccessToken: true,
    }),
};

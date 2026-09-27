import { sendApiRequest } from '../../api/httpClient';
import type { CreateTreeRequest, Tree, UpdateTreeRequest } from './treeDataTypes';

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
};

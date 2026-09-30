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

  deleteTreeWork: (projectId: number, treeId: number, workId: number) =>
    sendApiRequest<void>(`${treeWorksPath(projectId, treeId)}/${workId}`, {
      httpMethod: 'DELETE', sendAccessToken: true,
    }),
};

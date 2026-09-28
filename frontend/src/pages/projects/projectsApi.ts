import { sendApiRequest } from '../../api/httpClient';
import type { CreateProjectRequest, Project, UpdateProjectRequest } from './projectDataTypes';

/**
 * Endpointy modulu projektow (/api/projects/**). Wlasnosci projektu pilnuje backend -
 * frontend nigdy nie wysyla ownerId i nie filtruje projektow sam.
 */
export const projectsApi = {
  listProjectsOfCurrentUser: () =>
    sendApiRequest<Project[]>('/api/projects', { sendAccessToken: true }),

  createProject: (createProjectRequest: CreateProjectRequest) =>
    sendApiRequest<Project>('/api/projects', {
      httpMethod: 'POST', requestBody: createProjectRequest, sendAccessToken: true,
    }),

  getProjectOfCurrentUser: (projectId: number) =>
    sendApiRequest<Project>(`/api/projects/${projectId}`, { sendAccessToken: true }),

  updateProjectOfCurrentUser: (projectId: number, updateProjectRequest: UpdateProjectRequest) =>
    sendApiRequest<Project>(`/api/projects/${projectId}`, {
      httpMethod: 'PATCH', requestBody: updateProjectRequest, sendAccessToken: true,
    }),
};

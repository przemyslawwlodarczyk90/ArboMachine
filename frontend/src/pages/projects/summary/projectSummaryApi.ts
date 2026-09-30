import { sendApiRequest } from '../../../api/httpClient';
import type { ProjectSummary } from './projectSummaryDataTypes';

/** GET /api/projects/{projectId}/summary - liczby liczone w bazie; wlasnosc projektu sprawdza backend (404). */
export const projectSummaryApi = {
  getProjectSummary: (projectId: number) =>
    sendApiRequest<ProjectSummary>(`/api/projects/${projectId}/summary`, { sendAccessToken: true }),
};

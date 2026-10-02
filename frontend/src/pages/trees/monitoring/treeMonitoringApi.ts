import { sendApiRequest } from '../../../api/httpClient';
import { offlineService } from '../../../offline/offlineService';
import type { TreeMonitoringRecord, TreeMonitoringRecordRequest } from './monitoringDataTypes';

const monitoringPath = (projectId: number, treeId: number) => `/api/projects/${projectId}/trees/${treeId}/monitoring`;

/**
 * Monitoring stanu drzewa (etap 29). Hierarchie (wlasny projekt -> drzewo -> wpis) sprawdza backend. Dodanie,
 * edycja i usuniecie wpisu dzialaja takze bez polaczenia (kolejka offline z etapu 26).
 */
export const treeMonitoringApi = {
  listRecords: (projectId: number, treeId: number) =>
    sendApiRequest<TreeMonitoringRecord[]>(monitoringPath(projectId, treeId), { sendAccessToken: true }),

  createRecord: (projectId: number, treeId: number, request: TreeMonitoringRecordRequest) =>
    offlineService.mutate<TreeMonitoringRecord>({ kind: 'CREATE_TREE_MONITORING_RECORD', projectId, treeId, body: request }),

  updateRecord: (projectId: number, treeId: number, monitoringRecordId: number, request: TreeMonitoringRecordRequest) =>
    offlineService.mutate<TreeMonitoringRecord>({ kind: 'UPDATE_TREE_MONITORING_RECORD', projectId, treeId, monitoringRecordId, body: request }),

  deleteRecord: (projectId: number, treeId: number, monitoringRecordId: number) =>
    offlineService.mutate<void>({ kind: 'DELETE_TREE_MONITORING_RECORD', projectId, treeId, monitoringRecordId }),

  /** Ostatnia kontrola kazdego drzewa projektu (lista drzew). */
  listLatestRecordsOfProject: (projectId: number) =>
    sendApiRequest<TreeMonitoringRecord[]>(`/api/projects/${projectId}/monitoring/latest`, { sendAccessToken: true }),
};

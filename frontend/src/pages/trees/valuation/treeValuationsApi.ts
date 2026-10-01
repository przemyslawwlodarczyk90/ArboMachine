import { sendApiRequest } from '../../../api/httpClient';
import { offlineService } from '../../../offline/offlineService';
import type { TreeValuation, TreeValuationRequest } from './valuationDataTypes';

/**
 * Waloryzacja drzew (/api/projects/{projectId}/trees/{treeId}/valuation). Zapis i usuniecie dzialaja takze bez
 * polaczenia (kolejka offline z etapu 26); odczyt bez polaczenia zwraca ostatnia kopie z urzadzenia.
 */
export const treeValuationsApi = {
  getTreeValuation: (projectId: number, treeId: number) =>
    sendApiRequest<TreeValuation>(`/api/projects/${projectId}/trees/${treeId}/valuation`, { sendAccessToken: true }),

  saveTreeValuation: (projectId: number, treeId: number, request: TreeValuationRequest) =>
    offlineService.mutate<TreeValuation>({ kind: 'UPSERT_TREE_VALUATION', projectId, treeId, body: request }),

  deleteTreeValuation: (projectId: number, treeId: number) =>
    offlineService.mutate<TreeValuation | undefined>({ kind: 'DELETE_TREE_VALUATION', projectId, treeId }),

  /** Waloryzacje drzew projektu (tylko drzewa z waloryzacja) - lista drzew i mapa. */
  listProjectValuations: (projectId: number) =>
    sendApiRequest<TreeValuation[]>(`/api/projects/${projectId}/valuations`, { sendAccessToken: true }),
};

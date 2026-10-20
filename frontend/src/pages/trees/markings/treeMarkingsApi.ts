import { sendApiRequest } from '../../../api/httpClient';
import { offlineService } from '../../../offline/offlineService';
import type { TreeMarkingDefinition, TreeMarkings } from './treeMarkingDataTypes';

/** Katalog jest staly w czasie dzialania backendu - pobierany raz na sesje strony (po bledzie - ponownie). */
let catalogRequest: Promise<TreeMarkingDefinition[]> | null = null;

/**
 * Oznaczenia drzew (etap 31). Zapis dziala takze bez polaczenia (kolejka offline, SET_TREE_MARKINGS);
 * odczyt bez polaczenia zwraca ostatnia kopie z urzadzenia.
 */
export const treeMarkingsApi = {
  getCatalog: (): Promise<TreeMarkingDefinition[]> => {
    if (!catalogRequest) {
      catalogRequest = sendApiRequest<TreeMarkingDefinition[]>('/api/tree-markings', { sendAccessToken: true })
        .catch((loadingError) => {
          catalogRequest = null;
          throw loadingError;
        });
    }
    return catalogRequest;
  },

  getTreeMarkings: (projectId: number, treeId: number) =>
    sendApiRequest<TreeMarkings>(`/api/projects/${projectId}/trees/${treeId}/markings`, { sendAccessToken: true }),

  setTreeMarkings: (projectId: number, treeId: number, codes: string[]) =>
    offlineService.mutate<TreeMarkings>({ kind: 'SET_TREE_MARKINGS', projectId, treeId, body: { codes } }),

  /** Oznaczenia drzew projektu (tylko drzewa z oznaczeniami) - mapa i lista drzew. */
  listProjectMarkings: (projectId: number) =>
    sendApiRequest<TreeMarkings[]>(`/api/projects/${projectId}/tree-markings`, { sendAccessToken: true }),
};

import { useSyncExternalStore } from 'react';
import { offlineService, type OfflineState } from './offlineService';

/** Stan pracy offline (polaczenie, zmiany w kolejce, synchronizacja) - komponenty odswiezaja sie przy kazdej zmianie. */
export function useOfflineState(): OfflineState {
  return useSyncExternalStore(offlineService.subscribe, offlineService.getState, offlineService.getState);
}

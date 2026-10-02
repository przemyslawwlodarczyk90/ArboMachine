import { ApiRequestError, registerApiReadCache, sendApiRequest } from '../api/httpClient';
import { createIndexedDbOfflineStore } from './indexedDbOfflineStore';
import { createMemoryOfflineStore } from './memoryOfflineStore';
import {
  discardChange, isLocalOnlyPath, queueChange, syncPendingChanges, toPendingChangeRequest, type QueuedChangeDraft,
} from './offlineQueue';
import type { OfflineStore, PendingChange } from './offlineTypes';

// Praca offline w przegladarce (etap 26). Jeden obiekt na aplikacje: stan polaczenia, kolejka zmian zalogowanego
// konta, synchronizacja i kopie odczytow podpiete do httpClient. Logika kolejki jest w offlineQueue (testy).

export type OfflineSyncStatus = 'idle' | 'syncing' | 'failed';

export interface OfflineState {
  /** navigator.onLine i ostatnia proba kontaktu z serwerem sie udala */
  isOnline: boolean;
  pendingChangeCount: number;
  /** drzewa (takze tymczasowe id < 0) z niewyslanymi zmianami - do oznaczen na liscie i w karcie */
  pendingTreeIds: ReadonlySet<number>;
  syncStatus: OfflineSyncStatus;
  failedChange: PendingChange | null;
  /** liczba zmian wyslanych przez ostatnia udana synchronizacje (komunikat "Zsynchronizowano") */
  lastSyncedCount: number;
  /** rosnie po kazdej synchronizacji z wyslanymi zmianami - ekrany pobieraja wtedy dane ponownie */
  syncGeneration: number;
}

/** Co ile ponawiamy synchronizacje, gdy sa zmiany w kolejce (przegladarka nie zawsze zglasza powrot sieci). */
const SYNC_RETRY_INTERVAL_MS = 30_000;

const browserOnline = () => (typeof navigator === 'undefined' ? true : navigator.onLine);

let storePromise: Promise<OfflineStore> | null = null;
const store = () => {
  storePromise ??= (typeof indexedDB === 'undefined' ? Promise.reject(new Error('no IndexedDB')) : createIndexedDbOfflineStore())
    .catch(() => createMemoryOfflineStore());
  return storePromise;
};

let currentUserEmail: string | null = null;
let isServerReachable = true;
let syncInProgress: Promise<void> | null = null;
let state: OfflineState = {
  isOnline: browserOnline(), pendingChangeCount: 0, pendingTreeIds: new Set(), syncStatus: 'idle', failedChange: null,
  lastSyncedCount: 0, syncGeneration: 0,
};
const listeners = new Set<() => void>();

function setState(partial: Partial<OfflineState>) {
  state = { ...state, ...partial };
  listeners.forEach((listener) => listener());
}

async function refreshPendingState() {
  const changes = currentUserEmail
    ? (await (await store()).listChanges()).filter((change) => change.ownerEmail === currentUserEmail)
    : [];
  setState({
    pendingChangeCount: changes.length,
    pendingTreeIds: new Set(changes.map((change) => change.treeId)),
    failedChange: changes.find((change) => change.failure) ?? null,
    syncStatus: state.syncStatus === 'syncing' ? 'syncing' : changes.some((change) => change.failure) ? 'failed' : 'idle',
  });
}

function setServerReachable(reachable: boolean) {
  if (isServerReachable === reachable && state.isOnline === (reachable && browserOnline())) return;
  isServerReachable = reachable;
  setState({ isOnline: reachable && browserOnline() });
  if (reachable) scheduleSync();
}

function scheduleSync() {
  if (state.pendingChangeCount > 0 && state.isOnline && state.syncStatus !== 'failed') void offlineService.syncNow();
}

export const offlineService = {
  subscribe(listener: () => void) {
    listeners.add(listener);
    return () => { listeners.delete(listener); };
  },

  getState: () => state,

  /** AuthenticationContext: zalogowane konto (null = wylogowanie - kopie odczytow sa kasowane, kolejka zostaje). */
  async setCurrentUser(email: string | null) {
    if (email === null && currentUserEmail !== null) await (await store()).clearCachedResponses();
    currentUserEmail = email;
    setState({ syncStatus: 'idle', lastSyncedCount: 0 });
    await refreshPendingState();
    scheduleSync();
  },

  /**
   * Zapis: online i bez zalegajacych zmian -> zwykle zadanie; bez sieci, przy zmianach w kolejce (kolejnosc!) albo
   * dla obiektu z tymczasowym id -> zapis na urzadzeniu i synchronizacja pozniej. Bledy walidacji online sa
   * zwracane jak dotad (formularz pokazuje je przy polach).
   */
  async mutate<T>(draft: Omit<QueuedChangeDraft, 'ownerEmail'>): Promise<T> {
    const change: PendingChange = { ...draft, ownerEmail: currentUserEmail ?? '', treeId: draft.treeId ?? 0, createdAt: '' };
    const sendOnline = () => {
      const request = toPendingChangeRequest(change, change.treeId, change.workId, change.monitoringRecordId);
      return sendApiRequest<T>(request.path, { httpMethod: request.httpMethod, requestBody: request.body, sendAccessToken: true });
    };
    if (!currentUserEmail) return sendOnline();
    const usesLocalId = (draft.treeId ?? 0) < 0 || (draft.workId ?? 0) < 0 || (draft.monitoringRecordId ?? 0) < 0;
    if (state.isOnline && browserOnline() && !usesLocalId && state.pendingChangeCount === 0) {
      try {
        return await sendOnline();
      } catch (requestError) {
        if (!(requestError instanceof ApiRequestError) || requestError.httpStatus !== 0) throw requestError;
      }
    }
    const optimisticResult = await queueChange(await store(), { ...draft, ownerEmail: currentUserEmail }) as T;
    await refreshPendingState();
    scheduleSync();
    return optimisticResult;
  },

  /** "Synchronizuj teraz" i automatyczne proby. Jedna synchronizacja naraz. */
  syncNow(): Promise<void> {
    if (!currentUserEmail) return Promise.resolve();
    syncInProgress ??= (async () => {
      setState({ syncStatus: 'syncing' });
      const outcome = await syncPendingChanges(await store(), currentUserEmail!, async (request) => {
        try {
          return await sendApiRequest(request.path, { httpMethod: request.httpMethod, requestBody: request.body, sendAccessToken: true });
        } catch (requestError) {
          // Bledy walidacji pol: konkretne komunikaty zamiast ogolnego "Popraw zaznaczone pola."
          const fieldMessages = requestError instanceof ApiRequestError ? Object.values(requestError.errorMessageByFieldName) : [];
          if (requestError instanceof ApiRequestError && fieldMessages.length > 0) {
            throw new ApiRequestError(requestError.httpStatus, fieldMessages.join(' '), requestError.errorMessageByFieldName);
          }
          throw requestError;
        }
      });
      setState({
        syncStatus: outcome.status === 'failed' ? 'failed' : 'idle',
        lastSyncedCount: outcome.syncedCount,
        syncGeneration: outcome.syncedCount > 0 ? state.syncGeneration + 1 : state.syncGeneration,
      });
      if (outcome.status === 'offline') setServerReachable(false);
      await refreshPendingState();
    })().finally(() => { syncInProgress = null; });
    return syncInProgress;
  },

  /** Ponowna proba po bledzie (np. po poprawieniu danych na serwerze albo chwilowym bledzie 5xx). */
  async retryFailedChange() {
    const failed = state.failedChange;
    if (failed) await (await store()).updateChange({ ...failed, failure: null });
    setState({ syncStatus: 'idle' });
    await refreshPendingState();
    await offlineService.syncNow();
  },

  /** Pominiecie zmiany, ktorej nie da sie wyslac (razem ze zmianami od niej zaleznymi). */
  async discardFailedChange() {
    const failed = state.failedChange;
    if (failed?.id !== undefined) await discardChange(await store(), failed.id);
    setState({ syncStatus: 'idle' });
    await refreshPendingState();
    scheduleSync();
  },

  /** Id z serwera dla drzewa utworzonego offline (po synchronizacji) - karta drzewa przechodzi na nowy adres. */
  async serverTreeIdOf(localTreeId: number): Promise<number | undefined> {
    return (await store()).readIdMapping('tree', localTreeId);
  },
};

/** Start warstwy offline: kopie odczytow w httpClient, nasluch zmian polaczenia, okresowe ponawianie synchronizacji. */
export function startOfflineSupport() {
  registerApiReadCache({
    load: async (path) => (await store()).readCachedResponse(path),
    save: (path, value) => { void store().then((offlineStore) => offlineStore.writeCachedResponse(path, value)); },
    isLocalOnly: isLocalOnlyPath,
    reportReachability: setServerReachable,
  });
  if (typeof window === 'undefined') return;
  // Powrot sieci: zawsze proba synchronizacji - takze gdy zadne zadanie nie zdazylo sie nie udac (przegladarka
  // od razu wiedziala, ze jest offline, wiec "reachability" sie nie zmienila i setServerReachable nic by nie zrobil).
  window.addEventListener('online', () => {
    setState({ isOnline: isServerReachable });
    setServerReachable(true);
    scheduleSync();
  });
  window.addEventListener('offline', () => setState({ isOnline: false }));
  window.setInterval(() => {
    if (state.pendingChangeCount > 0 && browserOnline() && state.syncStatus === 'idle') void offlineService.syncNow();
  }, SYNC_RETRY_INTERVAL_MS);
  void refreshPendingState();
}

import { ApiRequestError, registerApiReadCache, sendApiRequest } from '../api/httpClient';
import { createIndexedDbOfflineStore } from './indexedDbOfflineStore';
import { createMemoryOfflineStore } from './memoryOfflineStore';
import {
  CREATING_CHANGE_KINDS, discardChange, isLocalOnlyPath, queueChange, syncPendingChanges, toPendingChangeRequest,
  type QueuedChangeDraft,
} from './offlineQueue';
import {
  createClientUploadId, pendingPhotosOfTree, queuePhotoUpload, retryFailedPhotoUpload, syncPendingPhotoUploads,
  type PhotoFileLike,
} from './offlinePhotoQueue';
import type { OfflineStore, PendingChange, PendingPhotoUpload } from './offlineTypes';

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
  /** etap 33: zdjecia zapisane na urzadzeniu, czekajace na wyslanie (z bledem - takze) */
  pendingPhotoCount: number;
  /** etap 33: zdjecia, ktorych serwer nie przyjal (np. format) - czekaja na decyzje */
  failedPhotoCount: number;
  /** etap 33: rosnie przy kazdej zmianie kolejki zdjec - sekcja zdjec odswieza liste zdjec z urzadzenia */
  photoQueueGeneration: number;
}

/** Etap 33: wynik dodania zdjecia - wyslane od razu albo zapisane na urzadzeniu do wyslania pozniej. */
export type TreePhotoUploadResult<T> = { kind: 'uploaded'; photo: T } | { kind: 'queued'; pendingPhoto: PendingPhotoUpload };

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
  lastSyncedCount: 0, syncGeneration: 0, pendingPhotoCount: 0, failedPhotoCount: 0, photoQueueGeneration: 0,
};
const listeners = new Set<() => void>();

function setState(partial: Partial<OfflineState>) {
  state = { ...state, ...partial };
  listeners.forEach((listener) => listener());
}

async function refreshPendingState() {
  const offlineStore = await store();
  const changes = currentUserEmail
    ? (await offlineStore.listChanges()).filter((change) => change.ownerEmail === currentUserEmail)
    : [];
  const photos = currentUserEmail
    ? (await offlineStore.listPhotoUploads()).filter((photo) => photo.ownerEmail === currentUserEmail)
    : [];
  setState({
    pendingChangeCount: changes.length,
    pendingTreeIds: new Set([...changes.map((change) => change.treeId), ...photos.map((photo) => photo.treeId)]),
    pendingPhotoCount: photos.length,
    failedPhotoCount: photos.filter((photo) => photo.failure).length,
    photoQueueGeneration: state.photoQueueGeneration + 1,
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

/** Etap 33: czy jest cos do wyslania (zdjecia z bledem czekaja na decyzje, wiec sie nie licza). */
const hasSomethingToSend = () => state.pendingChangeCount > 0 || state.pendingPhotoCount > state.failedPhotoCount;

function scheduleSync() {
  if (hasSomethingToSend() && state.isOnline && state.syncStatus !== 'failed') void offlineService.syncNow();
}

/** Wyslanie pliku zdjecia z identyfikatorem uploadu (ten sam endpoint co online; powtorzenie nie tworzy duplikatu). */
function sendPhotoUpload<T>(projectId: number, treeId: number, file: Blob, fileName: string, clientUploadId: string) {
  const uploadFormData = new FormData();
  uploadFormData.append('file', file, fileName);
  uploadFormData.append('clientUploadId', clientUploadId);
  return sendApiRequest<T>(`/api/projects/${projectId}/trees/${treeId}/photos`, {
    httpMethod: 'POST', requestBody: uploadFormData, sendAccessToken: true,
  });
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
  async mutate<T>(requestedDraft: Omit<QueuedChangeDraft, 'ownerEmail'>): Promise<T> {
    // Etap 36: tworzenie zawsze z identyfikatorem urzadzenia - ten sam przy wysylce online i z kolejki, wiec zerwana
    // odpowiedz i ponowienie nie tworza duplikatu na serwerze.
    const draft = CREATING_CHANGE_KINDS.includes(requestedDraft.kind) && !requestedDraft.clientRequestId
      ? { ...requestedDraft, clientRequestId: createClientUploadId() } : requestedDraft;
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
      // Etap 33: zdjecia po zmianach - drzewa utworzone offline maja juz id z serwera. Blad jednej zmiany nie
      // wstrzymuje zdjec drzew, ktore juz sa na serwerze.
      const photoOutcome = outcome.status === 'offline'
        ? null
        : await syncPendingPhotoUploads(await store(), currentUserEmail!, (projectId, treeId, photo) =>
          sendPhotoUpload(projectId, treeId, photo.blob, photo.fileName, photo.clientUploadId));
      const sentCount = outcome.syncedCount + (photoOutcome?.uploadedCount ?? 0);
      setState({
        syncStatus: outcome.status === 'failed' ? 'failed' : 'idle',
        lastSyncedCount: sentCount,
        syncGeneration: sentCount > 0 ? state.syncGeneration + 1 : state.syncGeneration,
      });
      if (outcome.status === 'offline' || photoOutcome?.status === 'offline') setServerReachable(false);
      await refreshPendingState();
    })().finally(() => { syncInProgress = null; });
    return syncInProgress;
  },

  /** Ponowna proba po bledzie (np. po poprawieniu danych na serwerze albo chwilowym bledzie 5xx). */
  async retryFailedChange() {
    const failed = state.failedChange;
    // Etap 37: ponowienie po konflikcie (409) = swiadome nadpisanie zmian z innego urzadzenia (bez wersji danych).
    const body = failed?.body as Record<string, unknown> | undefined;
    const retriedChange = failed && failed.failure?.httpStatus === 409 && failed.kind === 'UPDATE_TREE' && body
      ? { ...failed, body: Object.fromEntries(Object.entries(body).filter(([field]) => field !== 'expectedDataVersion')) }
      : failed;
    if (retriedChange) await (await store()).updateChange({ ...retriedChange, failure: null });
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

  /**
   * Etap 33: dodanie zdjecia drzewa. Online (drzewo na serwerze, nic nie czeka w kolejce) - od razu wysylka
   * z identyfikatorem uploadu; bez polaczenia, przy zerwaniu w trakcie albo dla drzewa utworzonego offline - zapis
   * pliku na urzadzeniu i wysylka po polaczeniu. Bledy walidacji online (format, rozmiar) sa zwracane jak dotad.
   */
  async uploadTreePhoto<T>(projectId: number, treeId: number, file: Blob & PhotoFileLike): Promise<TreePhotoUploadResult<T>> {
    const clientUploadId = createClientUploadId();
    const canSendNow = state.isOnline && browserOnline() && treeId > 0 && state.pendingChangeCount === 0;
    if (canSendNow || !currentUserEmail) {
      try {
        return { kind: 'uploaded', photo: await sendPhotoUpload<T>(projectId, treeId, file, file.name, clientUploadId) };
      } catch (requestError) {
        if (!currentUserEmail || !(requestError instanceof ApiRequestError) || requestError.httpStatus !== 0) throw requestError;
      }
    }
    const pendingPhoto = await queuePhotoUpload(await store(), { ownerEmail: currentUserEmail, projectId, treeId, file, clientUploadId });
    await refreshPendingState();
    scheduleSync();
    return { kind: 'queued', pendingPhoto };
  },

  /** Etap 33: zdjecia drzewa czekajace na urzadzeniu (takze dodane, gdy drzewo mialo tymczasowe id). */
  async pendingPhotosOfTree(treeId: number): Promise<PendingPhotoUpload[]> {
    return currentUserEmail ? pendingPhotosOfTree(await store(), currentUserEmail, treeId) : [];
  },

  /** Etap 33: usuniecie zdjecia z urzadzenia przed wyslaniem (pomylka albo zdjecie odrzucone przez serwer). */
  async discardPendingPhoto(clientUploadId: string) {
    await (await store()).removePhotoUpload(clientUploadId);
    await refreshPendingState();
  },

  /** Etap 33: ponowienie zdjecia odrzuconego przez serwer. */
  async retryPendingPhoto(clientUploadId: string) {
    await retryFailedPhotoUpload(await store(), clientUploadId);
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
    if (hasSomethingToSend() && browserOnline() && state.syncStatus === 'idle') void offlineService.syncNow();
  }, SYNC_RETRY_INTERVAL_MS);
  void refreshPendingState();
}

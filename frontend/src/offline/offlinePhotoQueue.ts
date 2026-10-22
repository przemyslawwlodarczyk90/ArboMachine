// Zdjecia offline (etap 33): plik zdjecia czeka w magazynie urzadzenia (IndexedDB, Blob) i jest wysylany po
// polaczeniu - po zmianach z kolejki (najpierw drzewo utworzone offline musi dostac id z serwera). Usuwany z urzadzenia
// dopiero po potwierdzeniu serwera; clientUploadId chroni przed duplikatem, gdy odpowiedz zginie po zapisie.
// Modul bez React i bez DOM - testowany przez node --test z magazynem w pamieci.
import { treePhotosPath } from './offlineQueue.ts';
import type { OfflineStore, PendingChangeFailure, PendingPhotoUpload } from './offlineTypes.ts';

/** UUID v4 z przegladarki (bezpieczny kontekst) albo zapasowy z Math.random - wystarczy do odroznienia uploadow. */
export function createClientUploadId(): string {
  const cryptoObject = (globalThis as { crypto?: { randomUUID?: () => string } }).crypto;
  if (cryptoObject?.randomUUID) return cryptoObject.randomUUID();
  const hex = (length: number) => Array.from({ length }, () => Math.floor(Math.random() * 16).toString(16)).join('');
  return `${hex(8)}-${hex(4)}-4${hex(3)}-${(8 + Math.floor(Math.random() * 4)).toString(16)}${hex(3)}-${hex(12)}`;
}

export interface PhotoFileLike {
  name: string;
  type: string;
  size: number;
}

/** Zapis zdjecia na urzadzeniu (bez wysylania). */
export async function queuePhotoUpload(store: OfflineStore, draft: { ownerEmail: string; projectId: number; treeId: number;
  file: Blob & PhotoFileLike; clientUploadId?: string }, now: Date = new Date()): Promise<PendingPhotoUpload> {
  const pendingPhoto: PendingPhotoUpload = {
    clientUploadId: draft.clientUploadId ?? createClientUploadId(),
    ownerEmail: draft.ownerEmail,
    projectId: draft.projectId,
    treeId: draft.treeId,
    fileName: draft.file.name || 'zdjecie.jpg',
    mimeType: draft.file.type,
    sizeBytes: draft.file.size,
    capturedAt: now.toISOString(),
    blob: draft.file,
    failure: null,
  };
  await store.putPhotoUpload(pendingPhoto);
  return pendingPhoto;
}

/** Drzewo zdjecia na serwerze: dodatnie id albo id z mapowania drzewa utworzonego offline (undefined = jeszcze nie). */
async function serverTreeIdOf(store: OfflineStore, treeId: number): Promise<number | undefined> {
  return treeId > 0 ? treeId : store.readIdMapping('tree', treeId);
}

/**
 * Zdjecia konta czekajace przy danym drzewie - takze te dodane, gdy drzewo mialo jeszcze tymczasowe (ujemne) id,
 * a dzis ogladamy je pod prawdziwym id po synchronizacji.
 */
export async function pendingPhotosOfTree(store: OfflineStore, ownerEmail: string, treeId: number): Promise<PendingPhotoUpload[]> {
  const result: PendingPhotoUpload[] = [];
  for (const photo of await store.listPhotoUploads()) {
    if (photo.ownerEmail !== ownerEmail) continue;
    if (photo.treeId === treeId || (photo.treeId < 0 && treeId > 0 && await store.readIdMapping('tree', photo.treeId) === treeId)) {
      result.push(photo);
    }
  }
  return result;
}

/** Wyslanie jednego zdjecia; rzuca blad z httpStatus (0 = brak polaczenia) - jak ApiRequestError. */
export type PhotoUploadSender = (projectId: number, serverTreeId: number, photo: PendingPhotoUpload) => Promise<unknown>;

export interface PhotoSyncOutcome {
  status: 'completed' | 'offline';
  uploadedCount: number;
  /** zdjecia drzew, ktore nie sa jeszcze na serwerze - czekaja na synchronizacje zmian */
  waitingForTreeCount: number;
  failedCount: number;
}

const httpStatusOf = (error: unknown) =>
  (typeof error === 'object' && error !== null && 'httpStatus' in error ? Number((error as { httpStatus: unknown }).httpStatus) : -1);
const messageOf = (error: unknown) => (error instanceof Error ? error.message : 'Nieoczekiwany błąd.');

/**
 * Wysyla zdjecia konta po kolei (najstarsze pierwsze). Sukces -> zdjecie znika z urzadzenia, a odpowiedz serwera
 * trafia do kopii listy zdjec drzewa. Brak polaczenia -> koniec (reszta czeka). Blad serwera (np. zly format) ->
 * zdjecie zostaje z opisem bledu i nie jest ponawiane automatycznie; pozostale zdjecia ida dalej.
 */
export async function syncPendingPhotoUploads(store: OfflineStore, ownerEmail: string, send: PhotoUploadSender): Promise<PhotoSyncOutcome> {
  const outcome: PhotoSyncOutcome = { status: 'completed', uploadedCount: 0, waitingForTreeCount: 0, failedCount: 0 };
  const photos = (await store.listPhotoUploads()).filter((photo) => photo.ownerEmail === ownerEmail);
  for (const photo of photos) {
    if (photo.failure) {
      outcome.failedCount++;
      continue;
    }
    const serverTreeId = await serverTreeIdOf(store, photo.treeId);
    if (serverTreeId === undefined) {
      outcome.waitingForTreeCount++;
      continue;
    }
    let uploadedPhoto: unknown;
    try {
      uploadedPhoto = await send(photo.projectId, serverTreeId, photo);
    } catch (sendingError) {
      const httpStatus = httpStatusOf(sendingError);
      if (httpStatus === 0) return { ...outcome, status: 'offline' };
      const failure: PendingChangeFailure = { httpStatus, message: messageOf(sendingError) };
      await store.putPhotoUpload({ ...photo, failure });
      outcome.failedCount++;
      continue;
    }
    await appendToCachedPhotoList(store, photo.projectId, serverTreeId, uploadedPhoto);
    await store.removePhotoUpload(photo.clientUploadId);
    outcome.uploadedCount++;
  }
  return outcome;
}

/** Wyslane zdjecie dopisane do kopii listy zdjec drzewa (bez duplikatu, gdy serwer zwrocil istniejacy rekord). */
async function appendToCachedPhotoList(store: OfflineStore, projectId: number, treeId: number, uploadedPhoto: unknown) {
  if (typeof uploadedPhoto !== 'object' || uploadedPhoto === null || !('id' in uploadedPhoto)) return;
  const path = treePhotosPath(projectId, treeId);
  const cachedList = await store.readCachedResponse(path);
  if (!Array.isArray(cachedList)) return;
  const uploadedId = (uploadedPhoto as { id: unknown }).id;
  if (cachedList.some((cachedPhoto) => (cachedPhoto as { id?: unknown }).id === uploadedId)) return;
  await store.writeCachedResponse(path, [...cachedList, uploadedPhoto]);
}

/** Ponowienie zdjecia z bledem (np. po zmianie limitu na serwerze) - wraca do kolejki. */
export async function retryFailedPhotoUpload(store: OfflineStore, clientUploadId: string) {
  const photo = (await store.listPhotoUploads()).find((candidate) => candidate.clientUploadId === clientUploadId);
  if (photo) await store.putPhotoUpload({ ...photo, failure: null });
}

// Etap 40: tresc wczesniej wyslanych zdjec na urzadzeniu - podglad bez zasiegu. Pobrane zdjecie zostaje w IndexedDB
// (limit miejsca, najstarsze wypadaja pierwsze); przy tej samej wersji tresci nie pytamy serwera drugi raz.
// Modul bez importow wartosci - testowany przez node --test.
import type { CachedPhotoContent, OfflineStore } from './offlineTypes.ts';

/**
 * Limit V1 na zdjecia zapisane do podgladu bez zasiegu: 300 MB (ok. 40-100 zdjec z tabletu). Zdjecia czekajace na
 * wyslanie (etap 33) sa poza tym limitem - nigdy nie sa usuwane automatycznie.
 */
export const PHOTO_CONTENT_CACHE_LIMIT_BYTES = 300 * 1024 * 1024;

export type PhotoContentKind = 'original' | 'documentation';

export const photoContentCacheKey = (projectId: number, treeId: number, photoId: number, kind: PhotoContentKind) =>
  `${projectId}/${treeId}/${photoId}/${kind}`;

export interface PhotoContentRequest {
  key: string;
  projectId: number;
  /**
   * wersja tresci (np. data ostatniej adnotacji; null = bez adnotacji) - inna wersja niz zapisana oznacza, ze zdjecie
   * sie zmienilo i online trzeba je pobrac ponownie
   */
  contentVersion: string | null;
}

/** Bez polaczenia z serwerem (httpClient: ApiRequestError z httpStatus 0). */
const isConnectionError = (error: unknown) =>
  typeof error === 'object' && error !== null && (error as { httpStatus?: unknown }).httpStatus === 0;

/**
 * Ktore zapisane zdjecia usunac, zeby zmiescic nowe (najstarsze pierwsze). null = nowe zdjecie jest wieksze niz caly
 * limit - nie zapisujemy go wcale (zamiast czyscic wszystko inne).
 */
export function planPhotoContentEviction(entries: Pick<CachedPhotoContent, 'key' | 'sizeBytes' | 'storedAt'>[],
                                         incoming: { key: string; sizeBytes: number },
                                         limitBytes = PHOTO_CONTENT_CACHE_LIMIT_BYTES): string[] | null {
  if (incoming.sizeBytes > limitBytes) return null;
  const others = entries.filter((entry) => entry.key !== incoming.key)
    .sort((first, second) => first.storedAt.localeCompare(second.storedAt));
  let usedBytes = others.reduce((sum, entry) => sum + entry.sizeBytes, 0);
  const keysToRemove: string[] = [];
  for (const entry of others) {
    if (usedBytes + incoming.sizeBytes <= limitBytes) break;
    keysToRemove.push(entry.key);
    usedBytes -= entry.sizeBytes;
  }
  return keysToRemove;
}

/** Zapis pobranego zdjecia z usunieciem najstarszych, jesli brakuje miejsca w limicie. Zwraca, czy zapisano. */
export async function storePhotoContent(store: OfflineStore, request: PhotoContentRequest, blob: Blob, storedAt: string,
                                        limitBytes = PHOTO_CONTENT_CACHE_LIMIT_BYTES): Promise<boolean> {
  const keysToRemove = planPhotoContentEviction(await store.listPhotoContentInfos(), { key: request.key, sizeBytes: blob.size }, limitBytes);
  if (keysToRemove === null) return false;
  if (keysToRemove.length > 0) await store.removePhotoContents(keysToRemove);
  await store.putPhotoContent({
    key: request.key, projectId: request.projectId, contentVersion: request.contentVersion, sizeBytes: blob.size, storedAt, blob,
  });
  return true;
}

/**
 * Tresc zdjecia: zapisana na urzadzeniu w tej samej wersji -> bez zapytania; inaczej z serwera (i zapis na urzadzeniu);
 * bez polaczenia -> ostatnia zapisana wersja, nawet starsza (lepszy starszy szkic niz pusty kafelek).
 */
export async function readThroughPhotoContent(store: OfflineStore, request: PhotoContentRequest,
                                              loadFromServer: () => Promise<Blob>,
                                              options: { now?: () => string; limitBytes?: number } = {}): Promise<Blob> {
  const cached = await store.readPhotoContent(request.key);
  if (cached && cached.contentVersion === request.contentVersion) return cached.blob;
  try {
    const blob = await loadFromServer();
    // Zapis nie moze zepsuc podgladu (np. brak miejsca w przegladarce) - zdjecie i tak jest pokazane.
    await storePhotoContent(store, request, blob, (options.now ?? (() => new Date().toISOString()))(), options.limitBytes)
      .catch(() => false);
    return blob;
  } catch (loadingError) {
    if (cached && isConnectionError(loadingError)) return cached.blob;
    throw loadingError;
  }
}

/**
 * "Przygotuj projekt" z pobraniem zdjec: zdjecia po kolei, dopoki mieszcza sie w limicie (reszta pominieta - nie
 * wypychamy wlasnie pobranych). Rozmiar z listy to rozmiar oryginalu - wersja z adnotacja moze sie roznic.
 */
export function selectPhotosWithinLimit<Photo extends { fileSizeBytes: number }>(photos: Photo[],
                                                                                 limitBytes = PHOTO_CONTENT_CACHE_LIMIT_BYTES) {
  const selected: Photo[] = [];
  let usedBytes = 0;
  for (const photo of photos) {
    if (usedBytes + photo.fileSizeBytes > limitBytes) continue;
    selected.push(photo);
    usedBytes += photo.fileSizeBytes;
  }
  return { selected, skippedCount: photos.length - selected.length, totalBytes: usedBytes };
}

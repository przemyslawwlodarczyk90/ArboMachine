// "Przygotuj projekt do pracy bez zasiegu" (etap 32). Odczyty API trafiaja do pamieci urzadzenia (httpClient +
// IndexedDB) dopiero wtedy, gdy ktos je pobierze - wiec przed wyjazdem pobieramy wszystko, czego uzywa tryb terenowy.
// Kolejnosc zapytan, rownoleglosc i podsumowanie - modul bez React, testowany przez node --test.
import type { KeyValueStorage } from '../map/basemapPreference.ts';
import { PHOTO_CONTENT_CACHE_LIMIT_BYTES, selectPhotosWithinLimit } from '../offline/photoContentCache.ts';

export type OfflinePreparationTask =
  | { kind: 'PROJECTS' | 'PROJECT' | 'TREES' | 'PROJECT_WORKS' | 'PROJECT_MARKINGS' | 'MARKING_CATALOG' | 'SYMBOL_CATALOG'
      | 'VALUATIONS' | 'STANDS' | 'REFERENCE_LAYERS' | 'PROJECT_PHOTOS' }
  | { kind: 'REFERENCE_LAYER_GEOJSON'; layerId: number }
  | { kind: 'TREE' | 'TREE_MARKINGS' | 'TREE_WORKS' | 'TREE_PHOTOS'; treeId: number }
  /** etap 40: tresc wczesniej wyslanego zdjecia (wersja do dokumentacji - ta, ktora pokazuje kafelek) */
  | { kind: 'PHOTO_CONTENT'; treeId: number; photoId: number; contentVersion: string | null };

/** Maks. 4 zapytania naraz - nie zapychamy slabego lacza ani serwera. */
export const MAX_CONCURRENT_REQUESTS = 4;
/** Od tej liczby drzew ostrzegamy, ze pobieranie potrwa (4 zapytania na drzewo). */
export const LARGE_PROJECT_TREE_COUNT = 500;

/** Etap 1: dane calego projektu (lista drzew i lista warstw daja id do etapu 2). */
export const PROJECT_LEVEL_TASKS: OfflinePreparationTask[] = [
  { kind: 'PROJECTS' }, { kind: 'PROJECT' }, { kind: 'TREES' }, { kind: 'PROJECT_WORKS' }, { kind: 'PROJECT_MARKINGS' },
  { kind: 'MARKING_CATALOG' }, { kind: 'SYMBOL_CATALOG' }, { kind: 'VALUATIONS' }, { kind: 'STANDS' }, { kind: 'REFERENCE_LAYERS' },
];

/** Zapytania na jedno drzewo w etapie 2 (do paska postepu). */
export const TREE_DETAIL_KINDS = ['TREE', 'TREE_MARKINGS', 'TREE_WORKS', 'TREE_PHOTOS'] as const;

/**
 * Etap 2: geometria warstw referencyjnych, potem karta, oznaczenia, prace i lista zdjec kazdego drzewa (drzewo po
 * drzewie). Lista zdjec (etap 40) zawsze - bez niej bez zasiegu sekcja zdjec pokazalaby blad zamiast listy.
 */
export function planDetailTasks(treeIds: number[], referenceLayerIds: number[]): OfflinePreparationTask[] {
  return [
    ...referenceLayerIds.map((layerId) => ({ kind: 'REFERENCE_LAYER_GEOJSON' as const, layerId })),
    ...treeIds.flatMap((treeId) => TREE_DETAIL_KINDS.map((kind) => ({ kind, treeId }))),
  ];
}

/** Zdjecie z listy zdjec projektu (GET /api/projects/{id}/photos) - tylko pola potrzebne do pobrania. */
export interface PreparablePhoto {
  photoId: number;
  treeId: number;
  fileSizeBytes: number;
  annotationUpdatedAt?: string | null;
}

/**
 * Etap 3 (etap 40, opcja "takze zdjecia"): tresc zdjec w limicie miejsca na urzadzeniu, w kolejnosci listy.
 * Zdjecia drzew utworzonych offline juz sa na urzadzeniu (kolejka wysylki) - lista z serwera ich nie zawiera.
 */
export function planPhotoContentTasks(photos: PreparablePhoto[], limitBytes = PHOTO_CONTENT_CACHE_LIMIT_BYTES) {
  const { selected, skippedCount, totalBytes } = selectPhotosWithinLimit(photos, limitBytes);
  const tasks: OfflinePreparationTask[] = selected.map((photo) => ({
    kind: 'PHOTO_CONTENT', treeId: photo.treeId, photoId: photo.photoId, contentVersion: photo.annotationUpdatedAt ?? null,
  }));
  return { tasks, skippedCount, totalBytes };
}

export interface TaskRunOutcome {
  completedCount: number;
  failedCount: number;
  isCancelled: boolean;
}

/**
 * Wykonuje zadania w kolejnosci listy, najwyzej `limit` naraz. Blad jednego zadania nie przerywa reszty (liczymy
 * bledy). Anulowanie: nowe zadania nie startuja, juz wyslane koncza sie normalnie.
 */
export async function runTasksWithConcurrency<Task>(tasks: Task[], limit: number, runTask: (task: Task) => Promise<unknown>,
                                                    options: { onProgress?: (doneCount: number) => void; isCancelled?: () => boolean } = {},
): Promise<TaskRunOutcome> {
  let nextIndex = 0;
  let completedCount = 0;
  let failedCount = 0;
  const worker = async () => {
    while (nextIndex < tasks.length && !options.isCancelled?.()) {
      const task = tasks[nextIndex];
      nextIndex += 1;
      try {
        await runTask(task);
        completedCount += 1;
      } catch {
        failedCount += 1;
      }
      options.onProgress?.(completedCount + failedCount);
    }
  };
  await Promise.all(Array.from({ length: Math.min(limit, tasks.length) }, worker));
  return { completedCount, failedCount, isCancelled: options.isCancelled?.() ?? false };
}

/** Polska odmiana liczebnika: 1 drzewo, 2-4 drzewa, 5+ drzew (12-14 tez "drzew"). */
export function polishCount(count: number, one: string, few: string, many: string): string {
  const lastDigit = count % 10;
  const lastTwoDigits = count % 100;
  const word = count === 1 ? one
    : lastDigit >= 2 && lastDigit <= 4 && !(lastTwoDigits >= 12 && lastTwoDigits <= 14) ? few : many;
  return `${count} ${word}`;
}

/** Etap 40: wynik pobierania zdjec; null = opcja "takze zdjecia" wylaczona. */
export interface PhotoPreparationSummary {
  downloadedCount: number;
  downloadedBytes: number;
  skippedCount: number;
}

export function describePreparationSummary(treeCount: number, layerCount: number, failedCount: number,
                                           photos: PhotoPreparationSummary | null = null): string {
  const photoText = photos === null
    ? ' Wcześniej wysłane zdjęcia - bez zasięgu tylko te już oglądane na tym urządzeniu.'
    : ` Zdjęcia: ${photos.downloadedCount} (${photos.downloadedBytes < 1024 * 1024 ? 'mniej niż 1 MB' : megabytes(photos.downloadedBytes)}).`
      + (photos.skippedCount > 0
        ? ` ${polishCount(photos.skippedCount, 'zdjęcie się nie zmieściło', 'zdjęcia się nie zmieściły', 'zdjęć się nie zmieściło')} w limicie ${megabytes(PHOTO_CONTENT_CACHE_LIMIT_BYTES)} na tym urządzeniu.`
        : '');
  const summary = `Gotowe: ${polishCount(treeCount, 'drzewo', 'drzewa', 'drzew')}, ${polishCount(layerCount, 'warstwa', 'warstwy', 'warstw')}.`
    + photoText + ' Podkład mapy nie jest dostępny bez zasięgu.';
  return failedCount > 0 ? `${summary} Nie udało się pobrać części danych (${failedCount}) - spróbuj ponownie przed wyjazdem.` : summary;
}

/**
 * Etap 33: przy mniej niz 300 MB wolnego miejsca ostrzegamy przed wyjazdem (zdjecie z tabletu to zwykle 3-8 MB, wiec
 * ok. 40-100 zdjec). Wartosc V1 do potwierdzenia, ile zdjec arborysta robi w ciagu dnia.
 */
export const LOW_STORAGE_WARNING_BYTES = 300 * 1024 * 1024;

const megabytes = (bytes: number) => `${Math.round(bytes / (1024 * 1024)).toLocaleString('pl-PL')} MB`;

/** Opis miejsca na urzadzeniu z navigator.storage.estimate(); null = przegladarka nie podaje szacunku. */
export function describeStorageEstimate(estimate: { usage?: number; quota?: number } | null): { text: string; isLow: boolean } | null {
  if (!estimate || estimate.quota === undefined || estimate.usage === undefined) return null;
  const freeBytes = Math.max(0, estimate.quota - estimate.usage);
  const isLow = freeBytes < LOW_STORAGE_WARNING_BYTES;
  return {
    text: `Miejsce dla aplikacji na tym urządzeniu: wolne ok. ${megabytes(freeBytes)} (zajęte ${megabytes(estimate.usage)}).`
      + (isLow ? ' To mało na zdjęcia z całego dnia - zwolnij miejsce albo wyślij zdjęcia przed wyjazdem.' : ''),
    isLow,
  };
}

const PREPARE_PHOTOS_KEY = 'arbomachine.field.preparePhotos';

/** Etap 40: czy "Przygotuj projekt" pobiera tez zdjecia (zapamietane na urzadzeniu; domyslnie nie - zajmuje miejsce). */
export function readPreparePhotosPreference(storage: KeyValueStorage | null): boolean {
  try {
    return storage?.getItem(PREPARE_PHOTOS_KEY) === 'true';
  } catch {
    return false;
  }
}

export function savePreparePhotosPreference(storage: KeyValueStorage | null, preparePhotos: boolean) {
  try {
    storage?.setItem(PREPARE_PHOTOS_KEY, String(preparePhotos));
  } catch {
    // Zablokowany magazyn - wybor obowiazuje do zamkniecia karty.
  }
}

const preparedAtKey = (projectId: number) => `arbomachine.field.offlinePreparedAt.${projectId}`;

export function readLastPreparationTime(storage: KeyValueStorage | null, projectId: number): string | null {
  try {
    const saved = storage?.getItem(preparedAtKey(projectId)) ?? null;
    return saved && !Number.isNaN(Date.parse(saved)) ? saved : null;
  } catch {
    return null;
  }
}

export function saveLastPreparationTime(storage: KeyValueStorage | null, projectId: number, preparedAt: string) {
  try {
    storage?.setItem(preparedAtKey(projectId), preparedAt);
  } catch {
    // Zablokowany magazyn - data przygotowania nie zostanie zapamietana, dane i tak sa w pamieci podrecznej.
  }
}

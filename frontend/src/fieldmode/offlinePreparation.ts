// "Przygotuj projekt do pracy bez zasiegu" (etap 32). Odczyty API trafiaja do pamieci urzadzenia (httpClient +
// IndexedDB) dopiero wtedy, gdy ktos je pobierze - wiec przed wyjazdem pobieramy wszystko, czego uzywa tryb terenowy.
// Kolejnosc zapytan, rownoleglosc i podsumowanie - modul bez React, testowany przez node --test.
import type { KeyValueStorage } from '../map/basemapPreference.ts';

export type OfflinePreparationTask =
  | { kind: 'PROJECTS' | 'PROJECT' | 'TREES' | 'PROJECT_WORKS' | 'PROJECT_MARKINGS' | 'MARKING_CATALOG' | 'SYMBOL_CATALOG'
      | 'VALUATIONS' | 'STANDS' | 'REFERENCE_LAYERS' }
  | { kind: 'REFERENCE_LAYER_GEOJSON'; layerId: number }
  | { kind: 'TREE' | 'TREE_MARKINGS' | 'TREE_WORKS'; treeId: number };

/** Maks. 4 zapytania naraz - nie zapychamy slabego lacza ani serwera. */
export const MAX_CONCURRENT_REQUESTS = 4;
/** Od tej liczby drzew ostrzegamy, ze pobieranie potrwa (3 zapytania na drzewo). */
export const LARGE_PROJECT_TREE_COUNT = 500;

/** Etap 1: dane calego projektu (lista drzew i lista warstw daja id do etapu 2). */
export const PROJECT_LEVEL_TASKS: OfflinePreparationTask[] = [
  { kind: 'PROJECTS' }, { kind: 'PROJECT' }, { kind: 'TREES' }, { kind: 'PROJECT_WORKS' }, { kind: 'PROJECT_MARKINGS' },
  { kind: 'MARKING_CATALOG' }, { kind: 'SYMBOL_CATALOG' }, { kind: 'VALUATIONS' }, { kind: 'STANDS' }, { kind: 'REFERENCE_LAYERS' },
];

/** Etap 2: geometria warstw referencyjnych, potem karta, oznaczenia i prace kazdego drzewa (drzewo po drzewie). */
export function planDetailTasks(treeIds: number[], referenceLayerIds: number[]): OfflinePreparationTask[] {
  return [
    ...referenceLayerIds.map((layerId) => ({ kind: 'REFERENCE_LAYER_GEOJSON' as const, layerId })),
    ...treeIds.flatMap((treeId) => (['TREE', 'TREE_MARKINGS', 'TREE_WORKS'] as const).map((kind) => ({ kind, treeId }))),
  ];
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

export function describePreparationSummary(treeCount: number, layerCount: number, failedCount: number): string {
  const summary = `Gotowe: ${polishCount(treeCount, 'drzewo', 'drzewa', 'drzew')}, ${polishCount(layerCount, 'warstwa', 'warstwy', 'warstw')}.`
    + ' Podkład mapy i zdjęcia nie są dostępne bez zasięgu.';
  return failedCount > 0 ? `${summary} Nie udało się pobrać części danych (${failedCount}) - spróbuj ponownie przed wyjazdem.` : summary;
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

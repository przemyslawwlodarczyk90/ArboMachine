import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { KeyValueStorage } from '../map/basemapPreference.ts';
import {
  describePreparationSummary, describeStorageEstimate, MAX_CONCURRENT_REQUESTS, planDetailTasks, planPhotoContentTasks, polishCount,
  PROJECT_LEVEL_TASKS, readLastPreparationTime, readPreparePhotosPreference, runTasksWithConcurrency, saveLastPreparationTime,
  savePreparePhotosPreference,
} from './offlinePreparation.ts';

// Uruchamianie: npm test (node --test). "Przygotuj projekt do pracy bez zasiegu" (etap 32).

test('etap 1 pobiera dane projektu, katalogi i listy; etap 2 - warstwy, potem drzewo po drzewie', () => {
  const kinds = PROJECT_LEVEL_TASKS.map((task) => task.kind);
  for (const kind of ['PROJECT', 'TREES', 'PROJECT_WORKS', 'PROJECT_MARKINGS', 'MARKING_CATALOG', 'SYMBOL_CATALOG', 'VALUATIONS',
    'STANDS', 'REFERENCE_LAYERS']) {
    assert.ok(kinds.includes(kind as never), kind);
  }
  assert.deepEqual(planDetailTasks([5, 7], [3]), [
    { kind: 'REFERENCE_LAYER_GEOJSON', layerId: 3 },
    { kind: 'TREE', treeId: 5 }, { kind: 'TREE_MARKINGS', treeId: 5 }, { kind: 'TREE_WORKS', treeId: 5 }, { kind: 'TREE_PHOTOS', treeId: 5 },
    { kind: 'TREE', treeId: 7 }, { kind: 'TREE_MARKINGS', treeId: 7 }, { kind: 'TREE_WORKS', treeId: 7 }, { kind: 'TREE_PHOTOS', treeId: 7 },
  ]);
});

test('etap 40: zdjecia do pobrania w limicie miejsca, z wersja adnotacji', () => {
  const plan = planPhotoContentTasks([
    { photoId: 1, treeId: 5, fileSizeBytes: 60, annotationUpdatedAt: '2026-10-22T10:00:00Z' },
    { photoId: 2, treeId: 5, fileSizeBytes: 60 },
    { photoId: 3, treeId: 7, fileSizeBytes: 30, annotationUpdatedAt: null },
  ], 100);
  assert.deepEqual(plan.tasks, [
    { kind: 'PHOTO_CONTENT', treeId: 5, photoId: 1, contentVersion: '2026-10-22T10:00:00Z' },
    { kind: 'PHOTO_CONTENT', treeId: 7, photoId: 3, contentVersion: null },
  ]);
  assert.equal(plan.skippedCount, 1);
});

test('etap 40: podsumowanie ze zdjeciami i z pominietymi', () => {
  assert.equal(describePreparationSummary(2, 0, 0, { downloadedCount: 5, downloadedBytes: 12 * 1024 * 1024, skippedCount: 0 }),
    'Gotowe: 2 drzewa, 0 warstw. Zdjęcia: 5 (12 MB). Podkład mapy nie jest dostępny bez zasięgu.');
  assert.match(describePreparationSummary(2, 0, 0, { downloadedCount: 1, downloadedBytes: 10, skippedCount: 3 }),
    /Zdjęcia: 1 \(mniej niż 1 MB\)\. 3 zdjęcia się nie zmieściły w limicie 300 MB/);
});

test('etap 40: wybor "takze zdjecia" zapamietany na urzadzeniu, domyslnie wylaczony', () => {
  const values = new Map<string, string>();
  const storage = { getItem: (key: string) => values.get(key) ?? null, setItem: (key: string, value: string) => { values.set(key, value); } };
  assert.equal(readPreparePhotosPreference(storage), false);
  savePreparePhotosPreference(storage, true);
  assert.equal(readPreparePhotosPreference(storage), true);
  assert.equal(readPreparePhotosPreference(null), false);
});

test('najwyzej 4 zapytania naraz, wszystkie wykonane, w kolejnosci startu z listy', async () => {
  let running = 0;
  let maxRunning = 0;
  const started: number[] = [];
  const progress: number[] = [];
  const outcome = await runTasksWithConcurrency(Array.from({ length: 10 }, (_, index) => index), MAX_CONCURRENT_REQUESTS, async (task) => {
    started.push(task);
    running += 1;
    maxRunning = Math.max(maxRunning, running);
    await new Promise((resolve) => setTimeout(resolve, 2));
    running -= 1;
  }, { onProgress: (done) => progress.push(done) });
  assert.equal(maxRunning, 4);
  assert.deepEqual(started, [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]);
  assert.deepEqual(outcome, { completedCount: 10, failedCount: 0, isCancelled: false });
  assert.equal(progress.at(-1), 10);
});

test('blad zadania nie przerywa reszty; anulowanie zatrzymuje nowe zadania', async () => {
  const failing = await runTasksWithConcurrency([1, 2, 3], 2, async (task) => { if (task === 2) throw new Error('x'); });
  assert.deepEqual(failing, { completedCount: 2, failedCount: 1, isCancelled: false });
  let isCancelled = false;
  let startedCount = 0;
  const cancelled = await runTasksWithConcurrency(Array.from({ length: 20 }, (_, index) => index), 4, async () => {
    startedCount += 1;
    if (startedCount === 4) isCancelled = true;
  }, { isCancelled: () => isCancelled });
  assert.equal(cancelled.isCancelled, true);
  assert.ok(startedCount < 20);
  assert.deepEqual(await runTasksWithConcurrency([], 4, async () => {}), { completedCount: 0, failedCount: 0, isCancelled: false });
});

test('podsumowanie po polsku z odmiana liczebnika', () => {
  assert.equal(describePreparationSummary(124, 3, 0), 'Gotowe: 124 drzewa, 3 warstwy. Wcześniej wysłane zdjęcia - bez zasięgu tylko te już oglądane na tym urządzeniu.'
    + ' Podkład mapy nie jest dostępny bez zasięgu.');
  assert.equal(polishCount(1, 'drzewo', 'drzewa', 'drzew'), '1 drzewo');
  assert.equal(polishCount(12, 'drzewo', 'drzewa', 'drzew'), '12 drzew');
  assert.equal(polishCount(0, 'warstwa', 'warstwy', 'warstw'), '0 warstw');
  assert.match(describePreparationSummary(1, 1, 2), /Nie udało się pobrać części danych \(2\)/);
});

test('miejsce na urzadzeniu: opis wolnego miejsca, ostrzezenie ponizej 300 MB, brak szacunku = null', () => {
  const plenty = describeStorageEstimate({ usage: 50 * 1024 * 1024, quota: 2048 * 1024 * 1024 });
  assert.equal(plenty?.isLow, false);
  assert.match(plenty!.text, /wolne ok\. 1\s?998 MB \(zajęte 50 MB\)/);
  const low = describeStorageEstimate({ usage: 900 * 1024 * 1024, quota: 1000 * 1024 * 1024 });
  assert.equal(low?.isLow, true);
  assert.match(low!.text, /zwolnij miejsce/);
  assert.equal(describeStorageEstimate(null), null);
  assert.equal(describeStorageEstimate({ usage: 1 }), null);
});

test('data ostatniego przygotowania zapamietana per projekt; uszkodzony wpis i wyjatek = brak daty', () => {
  const values: Record<string, string> = {};
  const storage: KeyValueStorage = { getItem: (key) => values[key] ?? null, setItem: (key, value) => { values[key] = value; } };
  assert.equal(readLastPreparationTime(storage, 5), null);
  saveLastPreparationTime(storage, 5, '2026-10-22T10:00:00.000Z');
  assert.equal(readLastPreparationTime(storage, 5), '2026-10-22T10:00:00.000Z');
  assert.equal(readLastPreparationTime(storage, 6), null);
  values['arbomachine.field.offlinePreparedAt.7'] = 'zle';
  assert.equal(readLastPreparationTime(storage, 7), null);
  const throwing: KeyValueStorage = { getItem: () => { throw new Error('x'); }, setItem: () => { throw new Error('x'); } };
  assert.equal(readLastPreparationTime(throwing, 5), null);
  assert.doesNotThrow(() => saveLastPreparationTime(throwing, 5, '2026-10-22T10:00:00.000Z'));
});

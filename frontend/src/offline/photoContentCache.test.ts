import assert from 'node:assert/strict';
import { test } from 'node:test';
import { createMemoryOfflineStore } from './memoryOfflineStore.ts';
import {
  photoContentCacheKey, planPhotoContentEviction, readThroughPhotoContent, selectPhotosWithinLimit, storePhotoContent,
  type PhotoContentRequest,
} from './photoContentCache.ts';

// Uruchamianie: npm test (node --test). Etap 40: zdjecia wyslane wczesniej - podglad bez zasiegu.

const photoBlob = (sizeBytes: number) => new Blob([new Uint8Array(sizeBytes)], { type: 'image/jpeg' });
const request = (photoId: number, contentVersion: string | null = null): PhotoContentRequest =>
  ({ key: photoContentCacheKey(7, 3, photoId, 'documentation'), projectId: 7, contentVersion });
const connectionError = () => Object.assign(new Error('Brak połączenia'), { httpStatus: 0 });

test('pierwszy podglad pobiera zdjecie z serwera i zapisuje je na urzadzeniu', async () => {
  const store = createMemoryOfflineStore();
  let serverCalls = 0;
  const blob = await readThroughPhotoContent(store, request(1), async () => { serverCalls += 1; return photoBlob(10); });
  assert.equal(blob.size, 10);
  assert.equal(serverCalls, 1);
  assert.equal((await store.readPhotoContent(request(1).key))?.sizeBytes, 10);
});

test('ta sama wersja tresci - bez ponownego zapytania do serwera', async () => {
  const store = createMemoryOfflineStore();
  let serverCalls = 0;
  const load = async () => { serverCalls += 1; return photoBlob(10); };
  await readThroughPhotoContent(store, request(1, '2026-10-22T10:00:00Z'), load);
  await readThroughPhotoContent(store, request(1, '2026-10-22T10:00:00Z'), load);
  assert.equal(serverCalls, 1);
});

test('nowa adnotacja (inna wersja) - online pobiera ponownie i zastepuje zapis', async () => {
  const store = createMemoryOfflineStore();
  await readThroughPhotoContent(store, request(1, null), async () => photoBlob(10));
  const blob = await readThroughPhotoContent(store, request(1, '2026-10-22T11:00:00Z'), async () => photoBlob(20));
  assert.equal(blob.size, 20);
  assert.equal((await store.readPhotoContent(request(1).key))?.contentVersion, '2026-10-22T11:00:00Z');
});

test('bez zasiegu - zapisana wersja, takze starsza niz aktualna', async () => {
  const store = createMemoryOfflineStore();
  await readThroughPhotoContent(store, request(1, null), async () => photoBlob(10));
  const blob = await readThroughPhotoContent(store, request(1, '2026-10-22T11:00:00Z'), async () => { throw connectionError(); });
  assert.equal(blob.size, 10);
});

test('bez zasiegu i bez zapisu - blad jak dotad (kafelek pokazuje brak podgladu)', async () => {
  const store = createMemoryOfflineStore();
  await assert.rejects(readThroughPhotoContent(store, request(1), async () => { throw connectionError(); }));
});

test('blad serwera (np. 404 - zdjecie usuniete) nie jest maskowany zapisana kopia', async () => {
  const store = createMemoryOfflineStore();
  await readThroughPhotoContent(store, request(1, null), async () => photoBlob(10));
  const notFound = Object.assign(new Error('Nie znaleziono'), { httpStatus: 404 });
  await assert.rejects(readThroughPhotoContent(store, request(1, 'nowa'), async () => { throw notFound; }), /Nie znaleziono/);
});

test('limit miejsca: najstarsze zdjecia wypadaja pierwsze', async () => {
  const store = createMemoryOfflineStore();
  await storePhotoContent(store, request(1), photoBlob(40), '2026-10-22T08:00:00Z', 100);
  await storePhotoContent(store, request(2), photoBlob(40), '2026-10-22T09:00:00Z', 100);
  await storePhotoContent(store, request(3), photoBlob(40), '2026-10-22T10:00:00Z', 100);
  const keys = (await store.listPhotoContentInfos()).map((info) => info.key).sort();
  assert.deepEqual(keys, [request(2).key, request(3).key]);
});

test('zdjecie wieksze niz caly limit nie jest zapisywane i nie wypycha innych', async () => {
  const store = createMemoryOfflineStore();
  await storePhotoContent(store, request(1), photoBlob(40), '2026-10-22T08:00:00Z', 100);
  assert.equal(await storePhotoContent(store, request(2), photoBlob(150), '2026-10-22T09:00:00Z', 100), false);
  assert.equal((await store.listPhotoContentInfos()).length, 1);
});

test('plan usuwania nie liczy podwojnie zastepowanego zdjecia', () => {
  const entries = [{ key: 'a', sizeBytes: 60, storedAt: '1' }, { key: 'b', sizeBytes: 30, storedAt: '2' }];
  assert.deepEqual(planPhotoContentEviction(entries, { key: 'a', sizeBytes: 70 }, 100), []);
  assert.deepEqual(planPhotoContentEviction(entries, { key: 'c', sizeBytes: 50 }, 100), ['a']);
});

test('przygotowanie projektu: zdjecia w limicie, reszta pominieta', () => {
  const photos = [{ id: 1, fileSizeBytes: 50 }, { id: 2, fileSizeBytes: 60 }, { id: 3, fileSizeBytes: 40 }];
  const result = selectPhotosWithinLimit(photos, 100);
  assert.deepEqual(result.selected.map((photo) => photo.id), [1, 3]);
  assert.equal(result.skippedCount, 1);
  assert.equal(result.totalBytes, 90);
});

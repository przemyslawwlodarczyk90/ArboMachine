import assert from 'node:assert/strict';
import { test } from 'node:test';
import { createMemoryOfflineStore } from './memoryOfflineStore.ts';
import {
  createClientUploadId, pendingPhotosOfTree, queuePhotoUpload, retryFailedPhotoUpload, syncPendingPhotoUploads,
  type PhotoUploadSender,
} from './offlinePhotoQueue.ts';
import { treePhotosPath } from './offlineQueue.ts';
import type { PendingPhotoUpload } from './offlineTypes.ts';

// Uruchamianie: npm test (node --test). Zdjecia offline (etap 33): kolejka plikow na urzadzeniu.

const OWNER = 'arborysta@example.com';
const PROJECT = 7;
const photoFile = (name = 'IMG_1.jpg') => Object.assign(new Blob([new Uint8Array([0xff, 0xd8, 0xff, 0xe0])], { type: 'image/jpeg' }), { name });

/** Falszywy serwer z idempotencja po clientUploadId (jak backend etapu 33). */
function fakeServer(options: { failWith?: number; offlineAfter?: number } = {}) {
  const storedByClientId = new Map<string, { id: number; treeId: number }>();
  const calls: { treeId: number; clientUploadId: string }[] = [];
  let nextId = 100;
  const send: PhotoUploadSender = async (_projectId, treeId, photo) => {
    calls.push({ treeId, clientUploadId: photo.clientUploadId });
    if (options.offlineAfter !== undefined && calls.length > options.offlineAfter) {
      throw Object.assign(new Error('Brak połączenia'), { httpStatus: 0 });
    }
    if (options.failWith) throw Object.assign(new Error('Nieobsługiwany format'), { httpStatus: options.failWith });
    const existing = storedByClientId.get(`${treeId}:${photo.clientUploadId}`);
    if (existing) return existing;
    const created = { id: nextId++, treeId };
    storedByClientId.set(`${treeId}:${photo.clientUploadId}`, created);
    return created;
  };
  return { send, calls, storedByClientId };
}

test('identyfikator uploadu to UUID; kazde zdjecie dostaje inny', () => {
  const first = createClientUploadId();
  assert.match(first, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
  assert.notEqual(first, createClientUploadId());
});

test('zdjecie zapisane na urzadzeniu z plikiem (Blob) i metadanymi', async () => {
  const store = createMemoryOfflineStore();
  const queued = await queuePhotoUpload(store, { ownerEmail: OWNER, projectId: PROJECT, treeId: 5, file: photoFile() },
    new Date('2026-10-22T10:00:00Z'));
  const [stored] = await store.listPhotoUploads();
  assert.equal(stored.clientUploadId, queued.clientUploadId);
  assert.equal(stored.fileName, 'IMG_1.jpg');
  assert.equal(stored.mimeType, 'image/jpeg');
  assert.equal(stored.sizeBytes, 4);
  assert.equal(stored.capturedAt, '2026-10-22T10:00:00.000Z');
  assert.equal((await stored.blob.arrayBuffer()).byteLength, 4);
});

test('wysylka po polaczeniu: zdjecie znika z urzadzenia dopiero po odpowiedzi serwera i trafia do kopii listy', async () => {
  const store = createMemoryOfflineStore();
  await store.writeCachedResponse(treePhotosPath(PROJECT, 5), [{ id: 1, treeId: 5 }]);
  await queuePhotoUpload(store, { ownerEmail: OWNER, projectId: PROJECT, treeId: 5, file: photoFile() });
  const server = fakeServer();
  const outcome = await syncPendingPhotoUploads(store, OWNER, server.send);
  assert.deepEqual(outcome, { status: 'completed', uploadedCount: 1, waitingForTreeCount: 0, failedCount: 0 });
  assert.deepEqual(await store.listPhotoUploads(), []);
  assert.deepEqual((await store.readCachedResponse(treePhotosPath(PROJECT, 5)) as { id: number }[]).map((photo) => photo.id), [1, 100]);
});

test('zerwane polaczenie w trakcie: zdjecia zostaja, ponowienie nie tworzy duplikatu na serwerze', async () => {
  const store = createMemoryOfflineStore();
  const first = await queuePhotoUpload(store, { ownerEmail: OWNER, projectId: PROJECT, treeId: 5, file: photoFile('a.jpg') },
    new Date('2026-10-22T10:00:00Z'));
  await queuePhotoUpload(store, { ownerEmail: OWNER, projectId: PROJECT, treeId: 5, file: photoFile('b.jpg') },
    new Date('2026-10-22T10:01:00Z'));
  const server = fakeServer({ offlineAfter: 1 });
  assert.equal((await syncPendingPhotoUploads(store, OWNER, server.send)).status, 'offline');
  assert.equal((await store.listPhotoUploads()).length, 1);
  // Serwer zapisal pierwsze zdjecie, ale aplikacja nie dostala odpowiedzi - zdjecie wraca do kolejki i jest ponawiane.
  await store.putPhotoUpload({ ...first });
  const retryServer = fakeServer();
  retryServer.storedByClientId.set(`5:${first.clientUploadId}`, { id: 100, treeId: 5 });
  await syncPendingPhotoUploads(store, OWNER, retryServer.send);
  assert.equal(retryServer.storedByClientId.size, 2);
  assert.deepEqual(await store.listPhotoUploads(), []);
});

test('zdjecie drzewa utworzonego offline czeka na id z serwera, potem idzie pod prawdziwe id', async () => {
  const store = createMemoryOfflineStore();
  await queuePhotoUpload(store, { ownerEmail: OWNER, projectId: PROJECT, treeId: -42, file: photoFile() });
  const server = fakeServer();
  assert.deepEqual(await syncPendingPhotoUploads(store, OWNER, server.send),
    { status: 'completed', uploadedCount: 0, waitingForTreeCount: 1, failedCount: 0 });
  assert.equal(server.calls.length, 0);
  assert.equal((await pendingPhotosOfTree(store, OWNER, -42)).length, 1);
  await store.writeIdMapping('tree', -42, 555);
  assert.equal((await pendingPhotosOfTree(store, OWNER, 555)).length, 1, 'widoczne pod prawdziwym id przed wysylka');
  await syncPendingPhotoUploads(store, OWNER, server.send);
  assert.deepEqual(server.calls.map((call) => call.treeId), [555]);
});

test('blad serwera (np. format): zdjecie zostaje z opisem, inne ida dalej, ponowienie na zadanie', async () => {
  const store = createMemoryOfflineStore();
  const rejected = await queuePhotoUpload(store, { ownerEmail: OWNER, projectId: PROJECT, treeId: 5, file: photoFile() });
  const failing = await syncPendingPhotoUploads(store, OWNER, fakeServer({ failWith: 400 }).send);
  assert.equal(failing.failedCount, 1);
  const [stored] = await store.listPhotoUploads() as PendingPhotoUpload[];
  assert.deepEqual(stored.failure, { httpStatus: 400, message: 'Nieobsługiwany format' });
  const server = fakeServer();
  await syncPendingPhotoUploads(store, OWNER, server.send);
  assert.equal(server.calls.length, 0, 'bez automatycznego ponawiania bledu');
  await retryFailedPhotoUpload(store, rejected.clientUploadId);
  await syncPendingPhotoUploads(store, OWNER, server.send);
  assert.deepEqual(await store.listPhotoUploads(), []);
});

test('zdjecia innego konta na tym samym urzadzeniu nie sa wysylane ani pokazywane', async () => {
  const store = createMemoryOfflineStore();
  await queuePhotoUpload(store, { ownerEmail: 'inne@example.com', projectId: PROJECT, treeId: 5, file: photoFile() });
  const server = fakeServer();
  await syncPendingPhotoUploads(store, OWNER, server.send);
  assert.equal(server.calls.length, 0);
  assert.deepEqual(await pendingPhotosOfTree(store, OWNER, 5), []);
});

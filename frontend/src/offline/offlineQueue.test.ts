import assert from 'node:assert/strict';
import { test } from 'node:test';
import { createMemoryOfflineStore } from './memoryOfflineStore.ts';
import {
  describePendingChange, discardChange, isLocalOnlyPath, projectWorksPath, queueChange, syncPendingChanges, treePath,
  treesListPath, treeWorksPath,
} from './offlineQueue.ts';
import type { PendingChangeRequest } from './offlineTypes.ts';

// Uruchamianie: npm test (node --test). Magazyn w pamieci zamiast IndexedDB i falszywy serwer zamiast fetch.

const OWNER = 'arborysta@example.com';
const PROJECT = 7;

/** Falszywy serwer: nadaje id, zapamietuje zadania; failAt - indeks zadania, ktore ma sie nie udac. */
function fakeServer(options: { failAt?: number; failure?: { httpStatus: number; message: string } } = {}) {
  const requests: PendingChangeRequest[] = [];
  let nextTreeId = 500;
  let nextWorkId = 900;
  const send = async (request: PendingChangeRequest) => {
    const index = requests.length;
    requests.push(request);
    if (options.failAt === index) {
      const error = new Error(options.failure?.message ?? 'Brak połączenia') as Error & { httpStatus: number };
      error.httpStatus = options.failure?.httpStatus ?? 0;
      throw error;
    }
    const body = (request.body ?? {}) as Record<string, unknown>;
    const treeIdInPath = Number(request.path.match(/\/trees\/(\d+)/)?.[1]);
    if (request.httpMethod === 'POST' && request.path.endsWith('/trees')) return { id: nextTreeId++, projectId: PROJECT, ...body, location: null };
    if (request.httpMethod === 'POST' && request.path.endsWith('/works')) return { id: nextWorkId++, treeId: treeIdInPath, ...body };
    if (request.path.endsWith('/location')) return { id: treeIdInPath, projectId: PROJECT, inventoryNumber: 'X', location: request.httpMethod === 'PUT' ? body : null };
    if (request.path.includes('/works/')) return { id: Number(request.path.split('/').pop()), treeId: treeIdInPath, ...body };
    return { id: treeIdInPath, projectId: PROJECT, ...body };
  };
  return { send, requests };
}

test('drzewo utworzone offline: tymczasowe ujemne id, od razu na liscie i w karcie, puste prace i zdjecia', async () => {
  const store = createMemoryOfflineStore();
  await store.writeCachedResponse(treesListPath(PROJECT), [{ id: 1, inventoryNumber: 'A-1' }]);
  const created = await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'CREATE_TREE', body: { inventoryNumber: 'N-1' } }) as { id: number; offlinePending: boolean; inventoryNumber: string; speciesPolish: unknown };
  assert.ok(created.id < 0);
  assert.equal(created.offlinePending, true);
  assert.equal(created.speciesPolish, null);
  const list = await store.readCachedResponse(treesListPath(PROJECT)) as { id: number }[];
  assert.deepEqual(list.map((tree) => tree.id), [1, created.id]);
  assert.equal((await store.readCachedResponse(treePath(PROJECT, created.id)) as { inventoryNumber: string }).inventoryNumber, 'N-1');
  assert.deepEqual(await store.readCachedResponse(treeWorksPath(PROJECT, created.id)), []);
  assert.equal(isLocalOnlyPath(treePath(PROJECT, created.id)), true);
  assert.equal(isLocalOnlyPath(treePath(PROJECT, 15)), false);
  assert.equal((await store.listChanges()).length, 1);
});

test('edycja, lokalizacja i praca offline nanoszone na kopie; synchronizacja FIFO z podmiana tymczasowych id', async () => {
  const store = createMemoryOfflineStore();
  await store.writeCachedResponse(treesListPath(PROJECT), []);
  await store.writeCachedResponse(projectWorksPath(PROJECT), []);
  const tree = await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'CREATE_TREE', body: { inventoryNumber: 'N-1' } }) as { id: number };
  await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'UPDATE_TREE', treeId: tree.id,
    body: { inventoryNumber: 'N-1', speciesPolish: 'lipa', heightM: 12.5 } });
  const located = await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'SET_TREE_LOCATION', treeId: tree.id,
    body: { latitude: 51.75, longitude: 19.45, accuracyM: 4, source: 'DEVICE_GNSS' } }) as { speciesPolish: string; location: { accuracyM: number } };
  assert.equal(located.speciesPolish, 'lipa', 'lokalizacja nie kasuje wczesniejszej edycji');
  assert.equal(located.location.accuracyM, 4);
  const work = await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'CREATE_TREE_WORK', treeId: tree.id, body: { name: 'Cięcia' } }) as { id: number };
  await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'UPDATE_TREE_WORK', treeId: tree.id, workId: work.id,
    body: { name: 'Cięcia sanitarne', description: 'posusz' } });
  assert.equal((await store.readCachedResponse(treeWorksPath(PROJECT, tree.id)) as { name: string }[])[0].name, 'Cięcia sanitarne');
  assert.equal((await store.readCachedResponse(projectWorksPath(PROJECT)) as unknown[]).length, 1);

  const server = fakeServer();
  const outcome = await syncPendingChanges(store, OWNER, server.send);
  assert.deepEqual(outcome, { status: 'completed', syncedCount: 5 });
  assert.deepEqual(server.requests.map((request) => `${request.httpMethod} ${request.path}`), [
    `POST /api/projects/${PROJECT}/trees`,
    `PATCH /api/projects/${PROJECT}/trees/500`,
    `PUT /api/projects/${PROJECT}/trees/500/location`,
    `POST /api/projects/${PROJECT}/trees/500/works`,
    `PATCH /api/projects/${PROJECT}/trees/500/works/900`,
  ]);
  assert.deepEqual(server.requests[2].body, { latitude: 51.75, longitude: 19.45, accuracyM: 4, source: 'DEVICE_GNSS' });
  assert.equal((await store.listChanges()).length, 0, 'kolejka pusta');
  assert.equal(await store.readIdMapping('tree', tree.id), 500);
  assert.equal(await store.readIdMapping('work', work.id), 900);
  const list = await store.readCachedResponse(treesListPath(PROJECT)) as { id: number; offlinePending?: boolean }[];
  assert.deepEqual(list.map((item) => item.id), [500], 'lokalne drzewo zastapione drzewem z serwera');
  assert.equal(list[0].offlinePending, undefined);
});

test('brak polaczenia w trakcie: synchronizacja staje, kolejka zachowana, ponowna proba konczy prace', async () => {
  const store = createMemoryOfflineStore();
  await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'UPDATE_TREE', treeId: 3, body: { inventoryNumber: 'A' } });
  await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'CLEAR_TREE_LOCATION', treeId: 3 });
  const offline = await syncPendingChanges(store, OWNER, fakeServer({ failAt: 1 }).send);
  assert.deepEqual(offline, { status: 'offline', syncedCount: 1 });
  const remaining = await store.listChanges();
  assert.deepEqual(remaining.map((change) => change.kind), ['CLEAR_TREE_LOCATION']);
  assert.equal(remaining[0].failure, null, 'brak sieci to nie blad zmiany');
  assert.deepEqual(await syncPendingChanges(store, OWNER, fakeServer().send), { status: 'completed', syncedCount: 1 });
});

test('blad walidacji zatrzymuje synchronizacje, zapisuje opis i nie czysci kolejki; pominiecie usuwa zmiany zalezne', async () => {
  const store = createMemoryOfflineStore();
  const tree = await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'CREATE_TREE', body: { inventoryNumber: '' } }) as { id: number };
  await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'SET_TREE_LOCATION', treeId: tree.id, body: { latitude: 1, longitude: 2 } });
  await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'UPDATE_TREE', treeId: 42, body: { inventoryNumber: 'B' } });
  const outcome = await syncPendingChanges(store, OWNER,
    fakeServer({ failAt: 0, failure: { httpStatus: 400, message: 'Numer inwentaryzacyjny jest wymagany.' } }).send);
  assert.equal(outcome.status, 'failed');
  if (outcome.status !== 'failed') return;
  assert.equal(outcome.failedChange.kind, 'CREATE_TREE');
  assert.deepEqual(outcome.failedChange.failure, { httpStatus: 400, message: 'Numer inwentaryzacyjny jest wymagany.' });
  assert.equal((await store.listChanges()).length, 3, 'nic nie usuniete');
  assert.equal(describePendingChange(outcome.failedChange), 'Nowe drzewo „”');

  assert.equal(await discardChange(store, outcome.failedChange.id!), 2, 'utworzenie + lokalizacja tego samego drzewa');
  const server = fakeServer();
  assert.deepEqual(await syncPendingChanges(store, OWNER, server.send), { status: 'completed', syncedCount: 1 });
  assert.deepEqual(server.requests.map((request) => request.path), [treePath(PROJECT, 42)]);
});

test('synchronizowane sa tylko zmiany zalogowanego konta', async () => {
  const store = createMemoryOfflineStore();
  await queueChange(store, { ownerEmail: 'inne.konto@example.com', projectId: PROJECT, kind: 'UPDATE_TREE', treeId: 9, body: { inventoryNumber: 'C' } });
  const server = fakeServer();
  assert.deepEqual(await syncPendingChanges(store, OWNER, server.send), { status: 'completed', syncedCount: 0 });
  assert.equal(server.requests.length, 0);
  assert.equal((await store.listChanges()).length, 1);
});

test('opisy zmian po polsku', () => {
  const base = { ownerEmail: OWNER, projectId: 1, treeId: 1, createdAt: '' };
  assert.equal(describePendingChange({ ...base, kind: 'CREATE_TREE', body: { inventoryNumber: 'A-1' } }), 'Nowe drzewo „A-1”');
  assert.equal(describePendingChange({ ...base, kind: 'SET_TREE_LOCATION' }), 'Lokalizacja drzewa');
  assert.equal(describePendingChange({ ...base, kind: 'CREATE_TREE_WORK', body: { name: 'Cięcia' } }), 'Nowa praca „Cięcia”');
});

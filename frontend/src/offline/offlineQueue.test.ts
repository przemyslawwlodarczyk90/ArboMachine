import assert from 'node:assert/strict';
import { test } from 'node:test';
import { createMemoryOfflineStore } from './memoryOfflineStore.ts';
import {
  describePendingChange, discardChange, isLocalOnlyPath, projectValuationsPath, projectWorksPath, queueChange, syncPendingChanges,
  projectLatestMonitoringPath, treeMonitoringPath, treePath, treesListPath, treeValuationPath, treeWorksPath,
  projectTreeMarkingsPath, treeMarkingsPath,
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
  let nextMonitoringRecordId = 700;
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
    if (request.httpMethod === 'POST' && request.path.endsWith('/monitoring')) return { id: nextMonitoringRecordId++, treeId: treeIdInPath, ...body };
    if (request.path.includes('/monitoring/')) return { id: Number(request.path.split('/').pop()), treeId: treeIdInPath, ...body };
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

test('waloryzacja offline: od razu w karcie i liscie projektu, synchronizacja PUT/DELETE z id drzewa z serwera', async () => {
  const store = createMemoryOfflineStore();
  await store.writeCachedResponse(projectValuationsPath(PROJECT), []);
  const tree = await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'CREATE_TREE', body: { inventoryNumber: 'W-1' } }) as { id: number };
  assert.deepEqual(await store.readCachedResponse(treeValuationPath(PROJECT, tree.id)), { treeId: tree.id, hasValuation: false });
  const saved = await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'UPSERT_TREE_VALUATION', treeId: tree.id,
    body: { overallCategory: 'HIGH', naturalValue: 'Okazały egzemplarz' } }) as Record<string, unknown>;
  assert.equal(saved.hasValuation, true);
  assert.equal(saved.offlinePending, true);
  assert.equal(saved.id, undefined);
  const list = await store.readCachedResponse(projectValuationsPath(PROJECT)) as { treeId: number; overallCategory: string }[];
  assert.deepEqual(list.map((item) => [item.treeId, item.overallCategory]), [[tree.id, 'HIGH']]);

  const sent: string[] = [];
  const outcome = await syncPendingChanges(store, OWNER, async (request) => {
    sent.push(`${request.httpMethod} ${request.path}`);
    if (request.path.endsWith('/trees')) return { id: 700, projectId: PROJECT, inventoryNumber: 'W-1' };
    return { treeId: 700, hasValuation: true, ...(request.body as object) };
  });
  assert.deepEqual(outcome, { status: 'completed', syncedCount: 2 });
  assert.deepEqual(sent, [`POST /api/projects/${PROJECT}/trees`, `PUT /api/projects/${PROJECT}/trees/700/valuation`]);
  const syncedList = await store.readCachedResponse(projectValuationsPath(PROJECT)) as { treeId: number }[];
  assert.deepEqual(syncedList.map((item) => item.treeId), [700], 'wpis z tymczasowym id zastapiony');

  await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'DELETE_TREE_VALUATION', treeId: 700 });
  assert.deepEqual(await store.readCachedResponse(projectValuationsPath(PROJECT)), []);
  assert.equal(describePendingChange((await store.listChanges())[0]), 'Usunięcie waloryzacji drzewa');
});

test('praca offline z etapu 28: status, priorytet i terminy w kopiach; usuniecie pracy offline (DELETE_TREE_WORK)', async () => {
  const store = createMemoryOfflineStore();
  const TREE = 41;
  await store.writeCachedResponse(treeWorksPath(PROJECT, TREE), [{ id: 300, treeId: TREE, name: 'Kontrola', status: 'RECOMMENDED' }]);
  await store.writeCachedResponse(projectWorksPath(PROJECT), [{ id: 300, treeId: TREE, name: 'Kontrola', status: 'RECOMMENDED' }]);
  const created = await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'CREATE_TREE_WORK', treeId: TREE,
    body: { name: 'Cięcia' } }) as Record<string, unknown>;
  assert.equal(created.status, 'RECOMMENDED', 'brak statusu = zalecenie, jak w backendzie');
  assert.equal(created.plannedDate, null);
  const updated = await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'UPDATE_TREE_WORK', treeId: TREE, workId: 300,
    body: { name: 'Kontrola', status: 'COMPLETED', priority: 'HIGH', completedDate: '2026-10-01' } }) as Record<string, unknown>;
  assert.deepEqual([updated.status, updated.priority, updated.completedDate, updated.plannedDate], ['COMPLETED', 'HIGH', '2026-10-01', null]);
  const projectWorks = await store.readCachedResponse(projectWorksPath(PROJECT)) as Record<string, unknown>[];
  assert.equal(projectWorks.find((work) => work.id === 300)?.status, 'COMPLETED', 'widok gospodarki widzi zmiane offline');

  await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'DELETE_TREE_WORK', treeId: TREE, workId: 300 });
  assert.deepEqual((await store.readCachedResponse(treeWorksPath(PROJECT, TREE)) as { id: number }[]).map((work) => work.id), [created.id]);
  assert.equal((await store.readCachedResponse(projectWorksPath(PROJECT)) as unknown[]).length, 1);
  assert.equal(describePendingChange((await store.listChanges())[2]), 'Usunięcie pracy');

  const server = fakeServer();
  assert.deepEqual(await syncPendingChanges(store, OWNER, server.send), { status: 'completed', syncedCount: 3 });
  assert.deepEqual(server.requests.map((request) => `${request.httpMethod} ${request.path}`), [
    `POST /api/projects/${PROJECT}/trees/${TREE}/works`,
    `PATCH /api/projects/${PROJECT}/trees/${TREE}/works/300`,
    `DELETE /api/projects/${PROJECT}/trees/${TREE}/works/300`,
  ]);
  assert.deepEqual((await store.readCachedResponse(treeWorksPath(PROJECT, TREE)) as { id: number }[]).map((work) => work.id), [900],
    'po synchronizacji usunieta praca nie wraca do kopii');
});

test('kontrola stanu offline (etap 29): od razu w historii i ostatniej kontroli projektu; synchronizacja POST/PUT/DELETE', async () => {
  const store = createMemoryOfflineStore();
  const TREE = 41;
  await store.writeCachedResponse(treeMonitoringPath(PROJECT, TREE), [{ id: 300, treeId: TREE, inspectionDate: '2026-05-01', healthStatus: 'GOOD' }]);
  await store.writeCachedResponse(projectLatestMonitoringPath(PROJECT), [{ id: 300, treeId: TREE, inspectionDate: '2026-05-01', healthStatus: 'GOOD' }]);

  const created = await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'CREATE_TREE_MONITORING_RECORD', treeId: TREE,
    body: { inspectionDate: '2026-10-02', healthStatus: 'POOR', notes: 'Pęknięcie' } }) as Record<string, unknown> & { id: number };
  assert.ok(created.id < 0);
  assert.equal(created.offlinePending, true);
  const history = await store.readCachedResponse(treeMonitoringPath(PROJECT, TREE)) as { id: number; inspectionDate: string }[];
  assert.deepEqual(history.map((record) => record.inspectionDate), ['2026-10-02', '2026-05-01'], 'najnowsza pierwsza');
  assert.equal((await store.readCachedResponse(projectLatestMonitoringPath(PROJECT)) as { healthStatus: string }[])[0].healthStatus, 'POOR');

  // edycja wpisu utworzonego offline (tymczasowe id) i usuniecie wpisu z serwera
  await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'UPDATE_TREE_MONITORING_RECORD', treeId: TREE,
    monitoringRecordId: created.id, body: { inspectionDate: '2026-10-02', healthStatus: 'FAIR' } });
  await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'DELETE_TREE_MONITORING_RECORD', treeId: TREE, monitoringRecordId: 300 });
  const afterDelete = await store.readCachedResponse(treeMonitoringPath(PROJECT, TREE)) as { id: number; healthStatus: string; notes: unknown }[];
  assert.deepEqual(afterDelete.map((record) => record.id), [created.id]);
  assert.equal(afterDelete[0].healthStatus, 'FAIR');
  assert.equal(afterDelete[0].notes, null, 'PUT = komplet pol, pominiete uwagi = brak');
  assert.equal(describePendingChange((await store.listChanges())[0]), 'Nowa kontrola stanu (2026-10-02)');
  assert.ok(isLocalOnlyPath(`${treeMonitoringPath(PROJECT, TREE)}/${created.id}`));

  const server = fakeServer();
  assert.deepEqual(await syncPendingChanges(store, OWNER, server.send), { status: 'completed', syncedCount: 3 });
  assert.deepEqual(server.requests.map((request) => `${request.httpMethod} ${request.path}`), [
    `POST /api/projects/${PROJECT}/trees/${TREE}/monitoring`,
    `PUT /api/projects/${PROJECT}/trees/${TREE}/monitoring/700`,
    `DELETE /api/projects/${PROJECT}/trees/${TREE}/monitoring/300`,
  ]);
  assert.deepEqual((await store.readCachedResponse(treeMonitoringPath(PROJECT, TREE)) as { id: number }[]).map((record) => record.id), [700],
    'po synchronizacji wpis ma id z serwera');
});

test('pominiecie kontroli utworzonej offline pomija tez jej edycje', async () => {
  const store = createMemoryOfflineStore();
  const created = await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'CREATE_TREE_MONITORING_RECORD', treeId: 41,
    body: { inspectionDate: '2026-10-02' } }) as { id: number };
  await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'UPDATE_TREE_MONITORING_RECORD', treeId: 41,
    monitoringRecordId: created.id, body: { inspectionDate: '2026-10-03' } });
  const [first] = await store.listChanges();
  assert.equal(await discardChange(store, first.id!), 2);
  assert.equal((await store.listChanges()).length, 0);
});

test('oznaczenia offline (etap 31): od razu w karcie i liscie projektu, synchronizacja PUT z id drzewa z serwera', async () => {
  const store = createMemoryOfflineStore();
  await store.writeCachedResponse(projectTreeMarkingsPath(PROJECT), [{ treeId: 3, codes: ['NEST'] }]);
  const tree = await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'CREATE_TREE', body: { inventoryNumber: 'O-1' } }) as { id: number };
  assert.deepEqual(await store.readCachedResponse(treeMarkingsPath(PROJECT, tree.id)), { treeId: tree.id, codes: [] });
  const saved = await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'SET_TREE_MARKINGS', treeId: tree.id,
    body: { codes: ['TO_REMOVE', 'SNAG'] } }) as Record<string, unknown>;
  assert.deepEqual(saved, { treeId: tree.id, codes: ['TO_REMOVE', 'SNAG'], offlinePending: true });
  const list = await store.readCachedResponse(projectTreeMarkingsPath(PROJECT)) as { treeId: number }[];
  assert.deepEqual(list.map((item) => item.treeId), [3, tree.id]);
  assert.equal(describePendingChange((await store.listChanges())[1]), 'Oznaczenia drzewa');

  const sent: string[] = [];
  const outcome = await syncPendingChanges(store, OWNER, async (request) => {
    sent.push(`${request.httpMethod} ${request.path}`);
    if (request.path.endsWith('/trees')) return { id: 800, projectId: PROJECT, inventoryNumber: 'O-1' };
    return { treeId: 800, ...(request.body as object) };
  });
  assert.deepEqual(outcome, { status: 'completed', syncedCount: 2 });
  assert.deepEqual(sent, [`POST /api/projects/${PROJECT}/trees`, `PUT /api/projects/${PROJECT}/trees/800/markings`]);
  const syncedList = await store.readCachedResponse(projectTreeMarkingsPath(PROJECT)) as { treeId: number }[];
  assert.deepEqual(syncedList.map((item) => item.treeId), [3, 800], 'wpis z tymczasowym id zastapiony');

  await queueChange(store, { ownerEmail: OWNER, projectId: PROJECT, kind: 'SET_TREE_MARKINGS', treeId: 3, body: { codes: [] } });
  assert.deepEqual((await store.readCachedResponse(projectTreeMarkingsPath(PROJECT)) as { treeId: number }[]).map((item) => item.treeId), [800]);
});

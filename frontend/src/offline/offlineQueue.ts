import type {
  IdMappingEntity, OfflineStore, PendingChange, PendingChangeFailure, PendingChangeKind, PendingChangeRequest,
} from './offlineTypes.ts';

// Rdzen pracy offline (etap 26): zapis zmiany na urzadzeniu z natychmiastowa (optymistyczna) aktualizacja kopii
// odczytow oraz synchronizacja FIFO istniejacymi endpointami. Bez React, bez DOM i bez fetch - magazyn i wysylanie
// sa podawane z zewnatrz, wiec calosc jest testowana przez node --test (magazyn w pamieci, falszywy serwer).

// ---------- sciezki API (te same, co w treesApi / treeWorksApi) ----------
export const treesListPath = (projectId: number) => `/api/projects/${projectId}/trees`;
export const treePath = (projectId: number, treeId: number) => `${treesListPath(projectId)}/${treeId}`;
export const treeWorksPath = (projectId: number, treeId: number) => `${treePath(projectId, treeId)}/works`;
export const treePhotosPath = (projectId: number, treeId: number) => `${treePath(projectId, treeId)}/photos`;
export const projectWorksPath = (projectId: number) => `/api/projects/${projectId}/works`;
export const treeValuationPath = (projectId: number, treeId: number) => `${treePath(projectId, treeId)}/valuation`;
export const projectValuationsPath = (projectId: number) => `/api/projects/${projectId}/valuations`;
export const treeMonitoringPath = (projectId: number, treeId: number) => `${treePath(projectId, treeId)}/monitoring`;
export const projectLatestMonitoringPath = (projectId: number) => `/api/projects/${projectId}/monitoring/latest`;

/** Pusta waloryzacja (jak GET backendu dla drzewa bez waloryzacji). */
const emptyValuationOf = (treeId: number) => ({ treeId, hasValuation: false });

/** Sciezki obiektow istniejacych tylko na urzadzeniu (tymczasowe id < 0) - nigdy nie ida do serwera. */
export const isLocalOnlyPath = (path: string) => /\/trees\/-\d+(\/|$)/.test(path) || /\/works\/-\d+(\/|$)/.test(path)
  || /\/monitoring\/-\d+(\/|$)/.test(path);

let localIdSequence = 0;
/** Tymczasowe id obiektu utworzonego offline: ujemne, unikalne na urzadzeniu. */
export const createLocalId = () => -(Date.now() * 100 + (localIdSequence++ % 100));

/** Pola drzewa ustawiane formularzem (komplet - brak pola = brak wartosci, jak w PATCH backendu). */
const TREE_EDITABLE_FIELDS = ['inventoryNumber', 'speciesLatin', 'speciesPolish', 'trunkCircumferenceCm', 'heightM',
  'crownDiameterM', 'notes', 'conditionNotes', 'hazardNotes'] as const;

/** Pola pracy ustawiane formularzem (etap 28: status, priorytet, terminy) - komplet, jak PATCH backendu. */
const WORK_EDITABLE_FIELDS = ['name', 'description', 'status', 'priority', 'plannedDate', 'completedDate'] as const;

const workFieldsOf = (body: Record<string, unknown>) => {
  const fields: Record<string, unknown> = {};
  for (const field of WORK_EDITABLE_FIELDS) fields[field] = body[field] ?? null;
  fields.status ??= 'RECOMMENDED';
  return fields;
};

/** Pola wpisu monitoringu (etap 29) - komplet, jak PUT backendu. */
const MONITORING_EDITABLE_FIELDS = ['inspectionDate', 'healthStatus', 'notes'] as const;

const monitoringFieldsOf = (body: Record<string, unknown>) => {
  const fields: Record<string, unknown> = {};
  for (const field of MONITORING_EDITABLE_FIELDS) fields[field] = body[field] ?? null;
  return fields;
};

type CachedRecord = Record<string, unknown> & { id: number };

export interface QueuedChangeDraft {
  ownerEmail: string;
  projectId: number;
  kind: PendingChangeKind;
  treeId?: number;
  workId?: number;
  monitoringRecordId?: number;
  body?: unknown;
}

/** Zadanie HTTP zmiany (id juz rozwiazane do id z serwera). */
export function toPendingChangeRequest(change: PendingChange, treeId: number, workId?: number,
                                       monitoringRecordId?: number): PendingChangeRequest {
  switch (change.kind) {
    case 'CREATE_TREE':
      return { httpMethod: 'POST', path: treesListPath(change.projectId), body: change.body };
    case 'UPDATE_TREE':
      return { httpMethod: 'PATCH', path: treePath(change.projectId, treeId), body: change.body };
    case 'SET_TREE_LOCATION':
      return { httpMethod: 'PUT', path: `${treePath(change.projectId, treeId)}/location`, body: change.body };
    case 'CLEAR_TREE_LOCATION':
      return { httpMethod: 'DELETE', path: `${treePath(change.projectId, treeId)}/location` };
    case 'CREATE_TREE_WORK':
      return { httpMethod: 'POST', path: treeWorksPath(change.projectId, treeId), body: change.body };
    case 'UPDATE_TREE_WORK':
      return { httpMethod: 'PATCH', path: `${treeWorksPath(change.projectId, treeId)}/${workId}`, body: change.body };
    case 'DELETE_TREE_WORK':
      return { httpMethod: 'DELETE', path: `${treeWorksPath(change.projectId, treeId)}/${workId}` };
    case 'UPSERT_TREE_VALUATION':
      return { httpMethod: 'PUT', path: treeValuationPath(change.projectId, treeId), body: change.body };
    case 'DELETE_TREE_VALUATION':
      return { httpMethod: 'DELETE', path: treeValuationPath(change.projectId, treeId) };
    case 'CREATE_TREE_MONITORING_RECORD':
      return { httpMethod: 'POST', path: treeMonitoringPath(change.projectId, treeId), body: change.body };
    case 'UPDATE_TREE_MONITORING_RECORD':
      return { httpMethod: 'PUT', path: `${treeMonitoringPath(change.projectId, treeId)}/${monitoringRecordId}`, body: change.body };
    case 'DELETE_TREE_MONITORING_RECORD':
      return { httpMethod: 'DELETE', path: `${treeMonitoringPath(change.projectId, treeId)}/${monitoringRecordId}` };
  }
}

// ---------- kopie odczytow ----------

async function readList(store: OfflineStore, path: string): Promise<CachedRecord[] | undefined> {
  const cached = await store.readCachedResponse(path);
  return Array.isArray(cached) ? (cached as CachedRecord[]) : undefined;
}

/** Zastepuje (albo dopisuje, gdy append) rekord o danym id w kopii listy - tylko gdy lista jest w pamieci urzadzenia. */
async function upsertInCachedList(store: OfflineStore, path: string, record: CachedRecord, replacedId: number, append: boolean) {
  const list = await readList(store, path);
  if (!list) return;
  const index = list.findIndex((item) => item.id === replacedId);
  if (index >= 0) list[index] = record;
  else if (append) list.push(record);
  else return;
  await store.writeCachedResponse(path, list);
}

/** Drzewo z odpowiedzi serwera (albo lokalne) zapisane w kopiach: karta drzewa i lista drzew projektu. */
async function cacheTree(store: OfflineStore, projectId: number, tree: CachedRecord, replacedId = tree.id, append = false) {
  await store.writeCachedResponse(treePath(projectId, tree.id), tree);
  await upsertInCachedList(store, treesListPath(projectId), tree, replacedId, append);
}

/**
 * Waloryzacja w kopiach: karta drzewa (GET .../valuation) i lista waloryzacji projektu (klucz = treeId, nie id -
 * lista zawiera tylko drzewa z waloryzacja).
 */
async function cacheValuation(store: OfflineStore, projectId: number, treeId: number, valuation: Record<string, unknown>,
                              replacedTreeId = treeId) {
  await store.writeCachedResponse(treeValuationPath(projectId, treeId), valuation);
  const list = await store.readCachedResponse(projectValuationsPath(projectId));
  if (!Array.isArray(list)) return;
  const others = (list as Record<string, unknown>[]).filter((item) => item.treeId !== replacedTreeId && item.treeId !== treeId);
  await store.writeCachedResponse(projectValuationsPath(projectId), valuation.hasValuation ? [...others, valuation] : others);
}

async function removeFromCachedList(store: OfflineStore, path: string, removedId: number) {
  const list = await readList(store, path);
  if (list) await store.writeCachedResponse(path, list.filter((item) => item.id !== removedId));
}

/** Historia kontroli drzewa: najnowsza pierwsza (data, potem kolejnosc dodania). */
const byInspectionNewestFirst = (first: CachedRecord, second: CachedRecord) =>
  String(second.inspectionDate ?? '').localeCompare(String(first.inspectionDate ?? '')) || Math.abs(second.id) - Math.abs(first.id);

/**
 * Wpis monitoringu w kopiach: historia drzewa (zastap / dopisz / usun) i "ostatnia kontrola" drzewa w liscie
 * projektu (przeliczana z historii). record null + removedId = usuniecie wpisu.
 */
async function cacheMonitoring(store: OfflineStore, projectId: number, treeId: number, record: CachedRecord | null,
                               replacedId: number, removedId?: number) {
  const path = treeMonitoringPath(projectId, treeId);
  const history = (await readList(store, path)) ?? [];
  // record.id tez - odpowiedz serwera na zmiane wpisu z tymczasowym id ma juz id z serwera.
  let updated = history.filter((item) => item.id !== replacedId && item.id !== removedId && item.id !== record?.id);
  if (record) updated = [...updated, record];
  updated.sort(byInspectionNewestFirst);
  await store.writeCachedResponse(path, updated);
  const latestList = await store.readCachedResponse(projectLatestMonitoringPath(projectId));
  if (!Array.isArray(latestList)) return;
  const others = (latestList as Record<string, unknown>[]).filter((item) => item.treeId !== treeId);
  await store.writeCachedResponse(projectLatestMonitoringPath(projectId), updated.length > 0 ? [...others, updated[0]] : others);
}

async function cacheWork(store: OfflineStore, projectId: number, treeId: number, work: CachedRecord, replacedId = work.id,
                         append = false) {
  await upsertInCachedList(store, treeWorksPath(projectId, treeId), work, replacedId, append);
  await upsertInCachedList(store, projectWorksPath(projectId), work, replacedId, append);
}

/**
 * Zapisuje zmiane w kolejce i od razu nanosi ja na kopie odczytow (lista drzew, karta drzewa, prace), wiec ekrany
 * pokazuja nowy stan bez polaczenia. Zwraca obiekt jak z backendu, oznaczony offlinePending.
 */
export async function queueChange(store: OfflineStore, queuedDraft: QueuedChangeDraft, now: Date = new Date()): Promise<unknown> {
  let draft = queuedDraft;
  const timestamp = now.toISOString();
  const projectId = draft.projectId;
  let treeId = draft.treeId ?? 0;
  let workId = draft.workId;
  let result: CachedRecord;

  switch (draft.kind) {
    case 'CREATE_TREE': {
      treeId = createLocalId();
      const body = (draft.body ?? {}) as Record<string, unknown>;
      result = { id: treeId, projectId, location: null, mapSymbolCode: null, createdAt: timestamp, updatedAt: timestamp, offlinePending: true };
      for (const field of TREE_EDITABLE_FIELDS) result[field] = body[field] ?? null;
      await cacheTree(store, projectId, result, treeId, true);
      // Puste sekcje drzewa utworzonego offline - karta drzewa dziala bez polaczenia.
      await store.writeCachedResponse(treeWorksPath(projectId, treeId), []);
      await store.writeCachedResponse(treePhotosPath(projectId, treeId), []);
      await store.writeCachedResponse(treeValuationPath(projectId, treeId), emptyValuationOf(treeId));
      await store.writeCachedResponse(treeMonitoringPath(projectId, treeId), []);
      break;
    }
    case 'CREATE_TREE_MONITORING_RECORD': {
      const recordId = createLocalId();
      draft = { ...draft, monitoringRecordId: recordId };
      result = { id: recordId, treeId, ...monitoringFieldsOf((draft.body ?? {}) as Record<string, unknown>),
        createdAt: timestamp, updatedAt: timestamp, offlinePending: true };
      await cacheMonitoring(store, projectId, treeId, result, recordId);
      break;
    }
    case 'UPDATE_TREE_MONITORING_RECORD': {
      const recordId = draft.monitoringRecordId ?? 0;
      const cachedRecord = ((await readList(store, treeMonitoringPath(projectId, treeId))) ?? []).find((item) => item.id === recordId)
        ?? { id: recordId, treeId, createdAt: timestamp };
      result = { ...cachedRecord, ...monitoringFieldsOf((draft.body ?? {}) as Record<string, unknown>), updatedAt: timestamp,
        offlinePending: true };
      await cacheMonitoring(store, projectId, treeId, result, recordId);
      break;
    }
    case 'DELETE_TREE_MONITORING_RECORD': {
      const recordId = draft.monitoringRecordId ?? 0;
      result = { id: recordId, treeId, offlinePending: true };
      await cacheMonitoring(store, projectId, treeId, null, recordId, recordId);
      break;
    }
    case 'UPSERT_TREE_VALUATION': {
      const body = (draft.body ?? {}) as Record<string, unknown>;
      const cachedValuation = (await store.readCachedResponse(treeValuationPath(projectId, treeId))) as Record<string, unknown> | undefined;
      result = { id: treeId, ...emptyValuationOf(treeId), ...body, treeId, hasValuation: true,
        createdAt: cachedValuation?.createdAt ?? timestamp, updatedAt: timestamp, offlinePending: true };
      delete (result as Record<string, unknown>).id;
      await cacheValuation(store, projectId, treeId, result);
      break;
    }
    case 'DELETE_TREE_VALUATION': {
      result = { id: treeId, ...emptyValuationOf(treeId), offlinePending: true };
      delete (result as Record<string, unknown>).id;
      await cacheValuation(store, projectId, treeId, result);
      break;
    }
    case 'UPDATE_TREE':
    case 'SET_TREE_LOCATION':
    case 'CLEAR_TREE_LOCATION': {
      const cachedTree = ((await store.readCachedResponse(treePath(projectId, treeId))) ?? { id: treeId, projectId }) as CachedRecord;
      result = { ...cachedTree, updatedAt: timestamp, offlinePending: true };
      if (draft.kind === 'UPDATE_TREE') {
        const body = (draft.body ?? {}) as Record<string, unknown>;
        for (const field of TREE_EDITABLE_FIELDS) result[field] = body[field] ?? null;
      } else {
        result.location = draft.kind === 'SET_TREE_LOCATION' ? draft.body : null;
      }
      await cacheTree(store, projectId, result);
      break;
    }
    case 'CREATE_TREE_WORK': {
      workId = createLocalId();
      const body = (draft.body ?? {}) as Record<string, unknown>;
      result = { id: workId, treeId, ...workFieldsOf(body), mapSymbolCode: null,
        createdAt: timestamp, updatedAt: timestamp, offlinePending: true };
      await cacheWork(store, projectId, treeId, result, workId, true);
      break;
    }
    case 'UPDATE_TREE_WORK': {
      const cachedWorks = (await readList(store, treeWorksPath(projectId, treeId))) ?? [];
      const body = (draft.body ?? {}) as Record<string, unknown>;
      const cachedWork = cachedWorks.find((work) => work.id === workId) ?? { id: workId ?? 0, treeId };
      result = { ...cachedWork, ...workFieldsOf(body), updatedAt: timestamp, offlinePending: true };
      await cacheWork(store, projectId, treeId, result);
      break;
    }
    case 'DELETE_TREE_WORK': {
      // Praca znika z kopii od razu; usuwanie pracy utworzonej offline (id < 0) UI nie oferuje przed synchronizacja.
      result = { id: workId ?? 0, treeId, offlinePending: true };
      await removeFromCachedList(store, treeWorksPath(projectId, treeId), result.id);
      await removeFromCachedList(store, projectWorksPath(projectId), result.id);
      break;
    }
  }

  await store.addChange({ ownerEmail: draft.ownerEmail, projectId, kind: draft.kind, treeId, workId,
    monitoringRecordId: draft.monitoringRecordId, body: draft.body, createdAt: timestamp, failure: null });
  return result;
}

// ---------- synchronizacja ----------

/** Wysylanie zadania do serwera; rzuca blad z httpStatus (0 = brak polaczenia) - jak ApiRequestError. */
export type PendingChangeSender = (request: PendingChangeRequest) => Promise<unknown>;

export type SyncOutcome =
  | { status: 'completed'; syncedCount: number }
  | { status: 'offline'; syncedCount: number }
  | { status: 'failed'; syncedCount: number; failedChange: PendingChange };

const httpStatusOf = (error: unknown) =>
  (typeof error === 'object' && error !== null && 'httpStatus' in error ? Number((error as { httpStatus: unknown }).httpStatus) : -1);
const messageOf = (error: unknown) => (error instanceof Error ? error.message : 'Nieoczekiwany błąd.');

async function resolveId(store: OfflineStore, entity: IdMappingEntity, id: number | undefined): Promise<number | undefined> {
  if (id === undefined || id > 0) return id;
  return store.readIdMapping(entity, id);
}

/**
 * Wysyla zmiany danego konta po kolei (FIFO). Sukces -> zmiana usunieta z kolejki, odpowiedz serwera trafia do kopii
 * (a dla CREATE_* zapisujemy mapowanie tymczasowego id). Brak polaczenia -> stop, kolejka bez zmian ("offline").
 * Inny blad -> zmiana dostaje opis bledu, synchronizacja staje ("failed") - dalsze zmiany moga od niej zalezec.
 * Konflikty: "ostatni zapis wygrywa" - PATCH wysyla komplet pol z urzadzenia.
 */
export async function syncPendingChanges(store: OfflineStore, ownerEmail: string, send: PendingChangeSender): Promise<SyncOutcome> {
  let syncedCount = 0;
  const changes = (await store.listChanges()).filter((change) => change.ownerEmail === ownerEmail);
  for (const change of changes) {
    const treeId = await resolveId(store, 'tree', change.treeId);
    const workId = await resolveId(store, 'work', change.workId);
    const monitoringRecordId = await resolveId(store, 'monitoring', change.monitoringRecordId);
    if (change.kind !== 'CREATE_TREE' && treeId === undefined) {
      const failedChange = { ...change, failure: { httpStatus: 0, message: 'Drzewo tej zmiany nie zostało jeszcze utworzone na serwerze.' } };
      await store.updateChange(failedChange);
      return { status: 'failed', syncedCount, failedChange };
    }
    let response: unknown;
    try {
      response = await send(toPendingChangeRequest(change, treeId ?? 0, workId, monitoringRecordId));
    } catch (sendingError) {
      const httpStatus = httpStatusOf(sendingError);
      if (httpStatus === 0) return { status: 'offline', syncedCount };
      const failure: PendingChangeFailure = { httpStatus, message: messageOf(sendingError) };
      const failedChange = { ...change, failure };
      await store.updateChange(failedChange);
      return { status: 'failed', syncedCount, failedChange };
    }
    await applyServerResponse(store, change, response);
    await store.removeChange(change.id!);
    syncedCount++;
  }
  return { status: 'completed', syncedCount };
}

async function applyServerResponse(store: OfflineStore, change: PendingChange, response: unknown) {
  if (change.kind === 'UPSERT_TREE_VALUATION' || change.kind === 'DELETE_TREE_VALUATION') {
    const serverTreeId = await resolveId(store, 'tree', change.treeId) ?? change.treeId;
    const valuation = change.kind === 'UPSERT_TREE_VALUATION' && typeof response === 'object' && response !== null
      ? (response as Record<string, unknown>) : emptyValuationOf(serverTreeId);
    await cacheValuation(store, change.projectId, serverTreeId, valuation, change.treeId);
    return;
  }
  if (change.kind === 'DELETE_TREE_MONITORING_RECORD') {
    const serverTreeId = await resolveId(store, 'tree', change.treeId) ?? change.treeId;
    const serverRecordId = await resolveId(store, 'monitoring', change.monitoringRecordId) ?? change.monitoringRecordId ?? 0;
    await cacheMonitoring(store, change.projectId, serverTreeId, null, serverRecordId, serverRecordId);
    return;
  }
  if (change.kind === 'CREATE_TREE_MONITORING_RECORD' || change.kind === 'UPDATE_TREE_MONITORING_RECORD') {
    if (typeof response !== 'object' || response === null || !('id' in response)) return;
    const record = response as CachedRecord;
    if (change.kind === 'CREATE_TREE_MONITORING_RECORD' && change.monitoringRecordId !== undefined) {
      await store.writeIdMapping('monitoring', change.monitoringRecordId, record.id);
    }
    await cacheMonitoring(store, change.projectId, Number(record.treeId), record, change.monitoringRecordId ?? record.id);
    return;
  }
  if (change.kind === 'DELETE_TREE_WORK') {
    // Kopie zaktualizowane przy zapisie; po synchronizacji usuwamy jeszcze z list pod id z serwera.
    const serverTreeId = await resolveId(store, 'tree', change.treeId) ?? change.treeId;
    const serverWorkId = await resolveId(store, 'work', change.workId) ?? change.workId ?? 0;
    await removeFromCachedList(store, treeWorksPath(change.projectId, serverTreeId), serverWorkId);
    await removeFromCachedList(store, projectWorksPath(change.projectId), serverWorkId);
    return;
  }
  if (typeof response !== 'object' || response === null || !('id' in response)) return;
  const record = response as CachedRecord;
  if (change.kind === 'CREATE_TREE') {
    await store.writeIdMapping('tree', change.treeId, record.id);
    await cacheTree(store, change.projectId, record, change.treeId);
    const localWorks = await store.readCachedResponse(treeWorksPath(change.projectId, change.treeId));
    await store.writeCachedResponse(treeWorksPath(change.projectId, record.id), localWorks ?? []);
    await store.writeCachedResponse(treePhotosPath(change.projectId, record.id), []);
    const localValuation = await store.readCachedResponse(treeValuationPath(change.projectId, change.treeId));
    await store.writeCachedResponse(treeValuationPath(change.projectId, record.id), localValuation ?? emptyValuationOf(record.id));
    const localMonitoring = await store.readCachedResponse(treeMonitoringPath(change.projectId, change.treeId));
    await store.writeCachedResponse(treeMonitoringPath(change.projectId, record.id), localMonitoring ?? []);
  } else if (change.kind === 'CREATE_TREE_WORK' || change.kind === 'UPDATE_TREE_WORK') {
    const serverTreeId = Number(record.treeId);
    if (change.kind === 'CREATE_TREE_WORK' && change.workId !== undefined) await store.writeIdMapping('work', change.workId, record.id);
    await cacheWork(store, change.projectId, serverTreeId, record, change.workId ?? record.id);
  } else {
    await cacheTree(store, change.projectId, record, record.id);
  }
}

/**
 * Pomija zmiane, ktorej nie da sie wyslac. Pominiecie utworzenia drzewa/pracy pomija tez zmiany zalezne od niego
 * (dotyczace tego samego obiektu tymczasowego) - inaczej i tak by nie przeszly. Zwraca liczbe pominietych zmian.
 */
export async function discardChange(store: OfflineStore, changeId: number): Promise<number> {
  const changes = await store.listChanges();
  const discarded = changes.find((change) => change.id === changeId);
  if (!discarded) return 0;
  const dependent = changes.filter((change) => change.id !== changeId && (
    (discarded.kind === 'CREATE_TREE' && change.treeId === discarded.treeId)
    || (discarded.kind === 'CREATE_TREE_WORK' && change.workId !== undefined && change.workId === discarded.workId)
    || (discarded.kind === 'CREATE_TREE_MONITORING_RECORD' && change.monitoringRecordId !== undefined
      && change.monitoringRecordId === discarded.monitoringRecordId)));
  for (const change of [discarded, ...dependent]) await store.removeChange(change.id!);
  return 1 + dependent.length;
}

/** Krotki polski opis zmiany do komunikatow ("Nowe drzewo „A-1”", "Lokalizacja drzewa"...). */
export function describePendingChange(change: PendingChange): string {
  const body = (change.body ?? {}) as Record<string, unknown>;
  switch (change.kind) {
    case 'CREATE_TREE': return `Nowe drzewo „${String(body.inventoryNumber ?? '')}”`;
    case 'UPDATE_TREE': return `Zmiana danych drzewa „${String(body.inventoryNumber ?? '')}”`;
    case 'SET_TREE_LOCATION': return 'Lokalizacja drzewa';
    case 'CLEAR_TREE_LOCATION': return 'Usunięcie lokalizacji drzewa';
    case 'CREATE_TREE_WORK': return `Nowa praca „${String(body.name ?? '')}”`;
    case 'UPDATE_TREE_WORK': return `Zmiana pracy „${String(body.name ?? '')}”`;
    case 'DELETE_TREE_WORK': return 'Usunięcie pracy';
    case 'UPSERT_TREE_VALUATION': return 'Waloryzacja drzewa';
    case 'DELETE_TREE_VALUATION': return 'Usunięcie waloryzacji drzewa';
    case 'CREATE_TREE_MONITORING_RECORD': return `Nowa kontrola stanu (${String(body.inspectionDate ?? '')})`;
    case 'UPDATE_TREE_MONITORING_RECORD': return `Zmiana kontroli stanu (${String(body.inspectionDate ?? '')})`;
    case 'DELETE_TREE_MONITORING_RECORD': return 'Usunięcie kontroli stanu';
  }
}

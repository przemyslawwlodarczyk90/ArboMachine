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

/** Pusta waloryzacja (jak GET backendu dla drzewa bez waloryzacji). */
const emptyValuationOf = (treeId: number) => ({ treeId, hasValuation: false });

/** Sciezki obiektow istniejacych tylko na urzadzeniu (tymczasowe id < 0) - nigdy nie ida do serwera. */
export const isLocalOnlyPath = (path: string) => /\/trees\/-\d+(\/|$)/.test(path) || /\/works\/-\d+(\/|$)/.test(path);

let localIdSequence = 0;
/** Tymczasowe id obiektu utworzonego offline: ujemne, unikalne na urzadzeniu. */
export const createLocalId = () => -(Date.now() * 100 + (localIdSequence++ % 100));

/** Pola drzewa ustawiane formularzem (komplet - brak pola = brak wartosci, jak w PATCH backendu). */
const TREE_EDITABLE_FIELDS = ['inventoryNumber', 'speciesLatin', 'speciesPolish', 'trunkCircumferenceCm', 'heightM',
  'crownDiameterM', 'notes', 'conditionNotes', 'hazardNotes'] as const;

type CachedRecord = Record<string, unknown> & { id: number };

export interface QueuedChangeDraft {
  ownerEmail: string;
  projectId: number;
  kind: PendingChangeKind;
  treeId?: number;
  workId?: number;
  body?: unknown;
}

/** Zadanie HTTP zmiany (id juz rozwiazane do id z serwera). */
export function toPendingChangeRequest(change: PendingChange, treeId: number, workId?: number): PendingChangeRequest {
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
    case 'UPSERT_TREE_VALUATION':
      return { httpMethod: 'PUT', path: treeValuationPath(change.projectId, treeId), body: change.body };
    case 'DELETE_TREE_VALUATION':
      return { httpMethod: 'DELETE', path: treeValuationPath(change.projectId, treeId) };
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

async function cacheWork(store: OfflineStore, projectId: number, treeId: number, work: CachedRecord, replacedId = work.id,
                         append = false) {
  await upsertInCachedList(store, treeWorksPath(projectId, treeId), work, replacedId, append);
  await upsertInCachedList(store, projectWorksPath(projectId), work, replacedId, append);
}

/**
 * Zapisuje zmiane w kolejce i od razu nanosi ja na kopie odczytow (lista drzew, karta drzewa, prace), wiec ekrany
 * pokazuja nowy stan bez polaczenia. Zwraca obiekt jak z backendu, oznaczony offlinePending.
 */
export async function queueChange(store: OfflineStore, draft: QueuedChangeDraft, now: Date = new Date()): Promise<unknown> {
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
      result = { id: workId, treeId, name: body.name, description: body.description ?? null, mapSymbolCode: null,
        createdAt: timestamp, updatedAt: timestamp, offlinePending: true };
      await cacheWork(store, projectId, treeId, result, workId, true);
      break;
    }
    case 'UPDATE_TREE_WORK': {
      const cachedWorks = (await readList(store, treeWorksPath(projectId, treeId))) ?? [];
      const body = (draft.body ?? {}) as Record<string, unknown>;
      const cachedWork = cachedWorks.find((work) => work.id === workId) ?? { id: workId ?? 0, treeId };
      result = { ...cachedWork, name: body.name, description: body.description ?? null, updatedAt: timestamp, offlinePending: true };
      await cacheWork(store, projectId, treeId, result);
      break;
    }
  }

  await store.addChange({ ownerEmail: draft.ownerEmail, projectId, kind: draft.kind, treeId, workId, body: draft.body,
    createdAt: timestamp, failure: null });
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
    if (change.kind !== 'CREATE_TREE' && treeId === undefined) {
      const failedChange = { ...change, failure: { httpStatus: 0, message: 'Drzewo tej zmiany nie zostało jeszcze utworzone na serwerze.' } };
      await store.updateChange(failedChange);
      return { status: 'failed', syncedCount, failedChange };
    }
    let response: unknown;
    try {
      response = await send(toPendingChangeRequest(change, treeId ?? 0, workId));
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
    || (discarded.kind === 'CREATE_TREE_WORK' && change.workId !== undefined && change.workId === discarded.workId)));
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
    case 'UPSERT_TREE_VALUATION': return 'Waloryzacja drzewa';
    case 'DELETE_TREE_VALUATION': return 'Usunięcie waloryzacji drzewa';
  }
}

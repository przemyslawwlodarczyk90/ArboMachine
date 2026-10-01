import type { IdMappingEntity, OfflineStore, PendingChange } from './offlineTypes.ts';

/**
 * Magazyn offline w pamieci - w testach i jako zapas, gdy przegladarka nie udostepnia IndexedDB (np. tryb prywatny
 * niektorych przegladarek): dziala do zamkniecia karty. Kopie wartosci (structuredClone) jak w IndexedDB.
 */
export function createMemoryOfflineStore(): OfflineStore {
  const responses = new Map<string, unknown>();
  const changes = new Map<number, PendingChange>();
  const idMappings = new Map<string, number>();
  let nextChangeId = 1;
  const copy = <T>(value: T): T => (value === undefined ? value : structuredClone(value));

  return {
    readCachedResponse: async (path) => copy(responses.get(path)),
    writeCachedResponse: async (path, value) => { responses.set(path, copy(value)); },
    clearCachedResponses: async () => { responses.clear(); },
    listChanges: async () => [...changes.values()].sort((first, second) => (first.id ?? 0) - (second.id ?? 0)).map(copy),
    addChange: async (change) => {
      const id = nextChangeId++;
      changes.set(id, copy({ ...change, id }));
      return id;
    },
    updateChange: async (change) => { if (change.id !== undefined) changes.set(change.id, copy(change)); },
    removeChange: async (changeId) => { changes.delete(changeId); },
    readIdMapping: async (entity: IdMappingEntity, localId) => idMappings.get(`${entity}:${localId}`),
    writeIdMapping: async (entity: IdMappingEntity, localId, serverId) => { idMappings.set(`${entity}:${localId}`, serverId); },
  };
}

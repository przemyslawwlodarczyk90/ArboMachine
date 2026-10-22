import type { CachedPhotoContent, IdMappingEntity, OfflineStore, PendingChange, PendingPhotoUpload } from './offlineTypes.ts';

/**
 * Magazyn offline w pamieci - w testach i jako zapas, gdy przegladarka nie udostepnia IndexedDB (np. tryb prywatny
 * niektorych przegladarek): dziala do zamkniecia karty. Kopie wartosci (structuredClone) jak w IndexedDB.
 */
export function createMemoryOfflineStore(): OfflineStore {
  const responses = new Map<string, unknown>();
  const changes = new Map<number, PendingChange>();
  const idMappings = new Map<string, number>();
  // Zdjecia bez structuredClone - Blob jest niezmienny, a kopia w Node nie jest potrzebna.
  const photoUploads = new Map<string, PendingPhotoUpload>();
  const photoContents = new Map<string, CachedPhotoContent>();
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
    listPhotoUploads: async () => [...photoUploads.values()]
      .sort((first, second) => first.capturedAt.localeCompare(second.capturedAt)).map((photo) => ({ ...photo })),
    putPhotoUpload: async (photo) => { photoUploads.set(photo.clientUploadId, { ...photo }); },
    removePhotoUpload: async (clientUploadId) => { photoUploads.delete(clientUploadId); },
    readPhotoContent: async (key) => { const content = photoContents.get(key); return content ? { ...content } : undefined; },
    putPhotoContent: async (content) => { photoContents.set(content.key, { ...content }); },
    listPhotoContentInfos: async () => [...photoContents.values()].map(({ blob: _blob, ...info }) => info),
    removePhotoContents: async (keys) => { keys.forEach((key) => photoContents.delete(key)); },
    clearPhotoContents: async () => { photoContents.clear(); },
  };
}

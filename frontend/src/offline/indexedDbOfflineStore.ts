import type { CachedPhotoContent, IdMappingEntity, OfflineStore, PendingChange, PendingPhotoUpload } from './offlineTypes';

const DATABASE_NAME = 'arbomachine-offline';
/** 2 = etap 33: magazyn zdjec czekajacych na wyslanie; 3 = etap 40: zdjecia do podgladu bez zasiegu. */
const DATABASE_VERSION = 3;
const RESPONSES = 'responses';
const CHANGES = 'pendingChanges';
const ID_MAPPINGS = 'idMappings';
const PHOTO_UPLOADS = 'photoUploads';
const PHOTO_CONTENTS = 'photoContents';

const requestResult = <T>(request: IDBRequest<T>) => new Promise<T>((resolve, reject) => {
  request.onsuccess = () => resolve(request.result);
  request.onerror = () => reject(request.error);
});

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const openRequest = indexedDB.open(DATABASE_NAME, DATABASE_VERSION);
    openRequest.onupgradeneeded = () => {
      const database = openRequest.result;
      if (!database.objectStoreNames.contains(RESPONSES)) database.createObjectStore(RESPONSES);
      if (!database.objectStoreNames.contains(CHANGES)) database.createObjectStore(CHANGES, { keyPath: 'id', autoIncrement: true });
      if (!database.objectStoreNames.contains(ID_MAPPINGS)) database.createObjectStore(ID_MAPPINGS);
      if (!database.objectStoreNames.contains(PHOTO_UPLOADS)) database.createObjectStore(PHOTO_UPLOADS, { keyPath: 'clientUploadId' });
      if (!database.objectStoreNames.contains(PHOTO_CONTENTS)) database.createObjectStore(PHOTO_CONTENTS, { keyPath: 'key' });
    };
    openRequest.onsuccess = () => resolve(openRequest.result);
    openRequest.onerror = () => reject(openRequest.error);
  });
}

/**
 * Magazyn offline w IndexedDB przegladarki (etap 26): kopie odpowiedzi GET (klucz = sciezka API), kolejka zmian
 * (autoIncrement = kolejnosc FIFO), mapowanie tymczasowych id, (etap 33) zdjecia jako Blob do czasu wyslania i (etap 40) zdjecia do podgladu bez zasiegu. Dane zostaja na urzadzeniu po zamknieciu karty;
 * kopie odczytow sa czyszczone przy wylogowaniu (dane klientow na wspoldzielonym tablecie).
 */
export async function createIndexedDbOfflineStore(): Promise<OfflineStore> {
  const database = await openDatabase();
  const run = async <T>(storeName: string, mode: IDBTransactionMode, operation: (store: IDBObjectStore) => IDBRequest<T>) =>
    requestResult(operation(database.transaction(storeName, mode).objectStore(storeName)));

  return {
    readCachedResponse: (path) => run(RESPONSES, 'readonly', (store) => store.get(path)),
    writeCachedResponse: async (path, value) => { await run(RESPONSES, 'readwrite', (store) => store.put(value, path)); },
    clearCachedResponses: async () => { await run(RESPONSES, 'readwrite', (store) => store.clear()); },
    listChanges: async () => (await run<PendingChange[]>(CHANGES, 'readonly', (store) => store.getAll()))
      .sort((first, second) => (first.id ?? 0) - (second.id ?? 0)),
    addChange: async (change) => {
      const { id: _ignoredId, ...changeWithoutId } = change;
      return Number(await run(CHANGES, 'readwrite', (store) => store.add(changeWithoutId)));
    },
    updateChange: async (change) => { await run(CHANGES, 'readwrite', (store) => store.put(change)); },
    removeChange: async (changeId) => { await run(CHANGES, 'readwrite', (store) => store.delete(changeId)); },
    readIdMapping: (entity: IdMappingEntity, localId) => run<number | undefined>(ID_MAPPINGS, 'readonly', (store) => store.get(`${entity}:${localId}`)),
    writeIdMapping: async (entity: IdMappingEntity, localId, serverId) => {
      await run(ID_MAPPINGS, 'readwrite', (store) => store.put(serverId, `${entity}:${localId}`));
    },
    listPhotoUploads: async () => (await run<PendingPhotoUpload[]>(PHOTO_UPLOADS, 'readonly', (store) => store.getAll()))
      .sort((first, second) => first.capturedAt.localeCompare(second.capturedAt)),
    putPhotoUpload: async (photo) => { await run(PHOTO_UPLOADS, 'readwrite', (store) => store.put(photo)); },
    removePhotoUpload: async (clientUploadId) => { await run(PHOTO_UPLOADS, 'readwrite', (store) => store.delete(clientUploadId)); },
    readPhotoContent: (key) => run<CachedPhotoContent | undefined>(PHOTO_CONTENTS, 'readonly', (store) => store.get(key)),
    putPhotoContent: async (content) => { await run(PHOTO_CONTENTS, 'readwrite', (store) => store.put(content)); },
    // getAll zwraca Blob jako uchwyt (tresc nie jest wczytywana do pamieci) - wystarczy do policzenia rozmiarow.
    listPhotoContentInfos: async () => (await run<CachedPhotoContent[]>(PHOTO_CONTENTS, 'readonly', (store) => store.getAll()))
      .map(({ blob: _blob, ...info }) => info),
    removePhotoContents: async (keys) => {
      const transaction = database.transaction(PHOTO_CONTENTS, 'readwrite');
      keys.forEach((key) => transaction.objectStore(PHOTO_CONTENTS).delete(key));
      await new Promise<void>((resolve, reject) => {
        transaction.oncomplete = () => resolve();
        transaction.onerror = () => reject(transaction.error);
      });
    },
    clearPhotoContents: async () => { await run(PHOTO_CONTENTS, 'readwrite', (store) => store.clear()); },
  };
}

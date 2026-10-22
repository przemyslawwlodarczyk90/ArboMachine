// Typy warstwy offline (etap 26). Modul bez importow wartosci - testowany przez node --test.

/**
 * Operacje, ktore V1 potrafi zapisac bez polaczenia i wyslac pozniej (te same endpointy co online).
 * Poza kolejka (wymagaja polaczenia): zdjecia, adnotacje, symbole mapowe, projekty, dokumenty, import, grupy drzew.
 */
export type PendingChangeKind =
  | 'CREATE_TREE'
  | 'UPDATE_TREE'
  | 'SET_TREE_LOCATION'
  | 'CLEAR_TREE_LOCATION'
  | 'CREATE_TREE_WORK'
  | 'UPDATE_TREE_WORK'
  | 'DELETE_TREE_WORK'
  /** etap 27: zapis (PUT - utworz albo zastap) i usuniecie waloryzacji drzewa */
  | 'UPSERT_TREE_VALUATION'
  | 'DELETE_TREE_VALUATION'
  /** etap 29: wpisy monitoringu (kontrole stanu) drzewa */
  | 'CREATE_TREE_MONITORING_RECORD'
  | 'UPDATE_TREE_MONITORING_RECORD'
  | 'DELETE_TREE_MONITORING_RECORD'
  /** etap 31: komplet oznaczen drzewa (PUT - zastepuje poprzednie) */
  | 'SET_TREE_MARKINGS';

/** Blad wyslania zmiany (walidacja, brak zasobu, blad serwera) - synchronizacja stoi do decyzji uzytkownika. */
export interface PendingChangeFailure {
  httpStatus: number;
  message: string;
}

/**
 * Zmiana zapisana na urzadzeniu. treeId / workId < 0 to tymczasowe id obiektow utworzonych offline - przy wysylaniu
 * zamieniane na id z serwera (mapowanie z odpowiedzi CREATE_*). ownerEmail - zmiany wysyla tylko to samo konto.
 */
export interface PendingChange {
  id?: number;
  ownerEmail: string;
  projectId: number;
  kind: PendingChangeKind;
  treeId: number;
  workId?: number;
  /** etap 29: wpis monitoringu (id < 0 = utworzony offline) */
  monitoringRecordId?: number;
  body?: unknown;
  createdAt: string;
  failure?: PendingChangeFailure | null;
}

/** Zadanie HTTP odpowiadajace zmianie (to samo, co wyslalby formularz online). */
export interface PendingChangeRequest {
  httpMethod: 'POST' | 'PATCH' | 'PUT' | 'DELETE';
  path: string;
  body?: unknown;
}

export type IdMappingEntity = 'tree' | 'work' | 'monitoring';

/**
 * Etap 33: zdjecie zrobione bez polaczenia (albo gdy wyslanie sie nie udalo) - plik jako Blob w IndexedDB do czasu
 * potwierdzenia serwera. treeId < 0 = drzewo utworzone offline (wysylka dopiero po jego utworzeniu na serwerze).
 * clientUploadId = UUID wysylany z plikiem - ponowienie po zerwanej odpowiedzi nie tworzy duplikatu na serwerze.
 */
export interface PendingPhotoUpload {
  clientUploadId: string;
  ownerEmail: string;
  projectId: number;
  treeId: number;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  /** czas dodania zdjecia na urzadzeniu (ISO) */
  capturedAt: string;
  blob: Blob;
  /** blad, ktorego ponowienie nie naprawi (np. zly format) - zdjecie czeka na decyzje uzytkownika */
  failure?: PendingChangeFailure | null;
}

/** Magazyn danych offline: kopie odpowiedzi GET, kolejka zmian, mapowanie tymczasowych id na id z serwera. */
export interface OfflineStore {
  readCachedResponse(path: string): Promise<unknown | undefined>;
  writeCachedResponse(path: string, value: unknown): Promise<void>;
  clearCachedResponses(): Promise<void>;
  listChanges(): Promise<PendingChange[]>;
  addChange(change: PendingChange): Promise<number>;
  updateChange(change: PendingChange): Promise<void>;
  removeChange(changeId: number): Promise<void>;
  readIdMapping(entity: IdMappingEntity, localId: number): Promise<number | undefined>;
  writeIdMapping(entity: IdMappingEntity, localId: number, serverId: number): Promise<void>;
  /** etap 33: zdjecia czekajace na wyslanie, w kolejnosci dodania */
  listPhotoUploads(): Promise<PendingPhotoUpload[]>;
  putPhotoUpload(photo: PendingPhotoUpload): Promise<void>;
  removePhotoUpload(clientUploadId: string): Promise<void>;
}

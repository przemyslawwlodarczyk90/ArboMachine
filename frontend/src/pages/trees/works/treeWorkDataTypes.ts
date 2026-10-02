// Typy modulu zalecen/prac przy drzewie - odpowiadaja DTO backendu w pakiecie domain.treework.dto.

/**
 * TreeWorkResponse. Nazwa to wolny tekst wpisany przez arboryste - celowo bez slownika typow prac
 * (slownik, kody i symbole mapowe nie sa jeszcze ustalone). Daty jako napisy ISO-8601 w UTC.
 */
export interface TreeWork {
  id: number;
  treeId: number;
  name: string;
  description?: string | null;
  /** symbol pracy na mapie (kod z GET /api/map-symbols); null = praca bez znaczka na mapie */
  mapSymbolCode?: string | null;
  /** etap 28: status pracy (prace sprzed etapu 28 backend zwraca jako RECOMMENDED) */
  status: TreeWorkStatus;
  /** etap 28: priorytet organizacyjny; null = nie okreslono */
  priority?: TreeWorkPriority | null;
  /** etap 28: termin i data wykonania - daty bez czasu, 'YYYY-MM-DD' */
  plannedDate?: string | null;
  completedDate?: string | null;
  createdAt: string;
  updatedAt: string;
  /** tylko frontend (etap 26): zapisane na urzadzeniu, czeka na synchronizacje; id < 0 = praca utworzona offline */
  offlinePending?: boolean;
}

/**
 * Body POST i PATCH .../works - zawsze komplet pol; pominiety opis oznacza jego brak (przy edycji - wyczyszczenie).
 * Bez projectId/treeId - pochodza z URL.
 */
export interface TreeWorkFieldsRequest {
  name: string;
  description?: string;
  status: TreeWorkStatus;
  priority?: TreeWorkPriority;
  plannedDate?: string;
  completedDate?: string;
}

/**
 * Status pracy V1 (etap 28) - robocze kody do uzgodnienia z arborystami. Dowolne przejscia (bez maszyny stanow):
 * RECOMMENDED - zalecenie arborysty, PLANNED - zaplanowana, COMPLETED - wykonana, CANCELLED - anulowana.
 */
export type TreeWorkStatus = 'RECOMMENDED' | 'PLANNED' | 'COMPLETED' | 'CANCELLED';

/** Priorytet organizacyjny V1 (etap 28) - nie jest branzowa skala pilnosci. */
export type TreeWorkPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';

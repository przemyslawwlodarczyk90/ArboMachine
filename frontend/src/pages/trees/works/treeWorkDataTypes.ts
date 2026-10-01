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
}

// Typy modulu drzew - odpowiadaja DTO backendu w pakiecie domain.tree.dto.

/** TreeResponse. Daty jako napisy ISO-8601 w UTC (java.time.Instant). */
export interface Tree {
  id: number;
  projectId: number;
  /** uzytkowy identyfikator nadawany przez arboryste - tekst, np. "1", "A-12" */
  inventoryNumber: string;
  speciesLatin?: string | null;
  speciesPolish?: string | null;
  /** obwod pnia w cm - liczba calkowita */
  trunkCircumferenceCm?: number | null;
  /** wysokosc w metrach, max 2 miejsca po przecinku */
  heightM?: number | null;
  /** srednica korony w metrach (jeden wymiar), max 2 miejsca po przecinku */
  crownDiameterM?: number | null;
  notes?: string | null;
  /** opis stanu drzewa - V1: wolny tekst */
  conditionNotes?: string | null;
  /** zagrozenia stwarzane przez drzewo - V1: wolny tekst */
  hazardNotes?: string | null;
  /** null = lokalizacja drzewa nie jest jeszcze ustalona (nigdy 0,0 ani jedna wspolrzedna) */
  location?: TreeLocation | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * TreeLocationResponse - punkt drzewa w stopniach dziesietnych (WGS84), zawsze kompletna para.
 * Przyszle dane pomiaru (dokladnosc, zrodlo) dojda tutaj, a nie jako luzne pola drzewa.
 */
export interface TreeLocation {
  latitude: number;
  longitude: number;
}

/**
 * Body PUT /api/projects/{projectId}/trees/{treeId}/location. Backend nie wie, skad pochodza wspolrzedne
 * (formularz, mapa, GNSS) - usuniecie lokalizacji to osobne DELETE, a nie wyslanie pustych pol.
 */
export type TreeLocationRequest = TreeLocation;

/**
 * Pola drzewa ustawiane przez uzytkownika (TreeEditableFieldsRequest w backendzie) - bez projectId (idzie w URL)
 * i bez danych wlasciciela. Formularze zawsze wysylaja komplet pol; pominiete pole opcjonalne oznacza brak wartosci.
 */
export interface TreeEditableFieldsRequest {
  inventoryNumber: string;
  speciesLatin?: string;
  speciesPolish?: string;
  trunkCircumferenceCm?: number;
  heightM?: number;
  crownDiameterM?: number;
  notes?: string;
  conditionNotes?: string;
  hazardNotes?: string;
}

/** Body POST /api/projects/{projectId}/trees. */
export type CreateTreeRequest = TreeEditableFieldsRequest;

/** Body PATCH /api/projects/{projectId}/trees/{treeId} - pominiete pole opcjonalne czysci wartosc na backendzie. */
export type UpdateTreeRequest = TreeEditableFieldsRequest;

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
  /** symbol drzewa na mapie (kod z GET /api/map-symbols); null = neutralny marker */
  mapSymbolCode?: string | null;
  createdAt: string;
  updatedAt: string;
  /** etap 37: wersja danych drzewa (pol formularza) - odsylana przy zapisie, zeby wykryc zmiane z innego urzadzenia */
  dataVersion?: number;
  /** tylko frontend (etap 26): zapisane na urzadzeniu, czeka na synchronizacje; id < 0 = drzewo utworzone offline */
  offlinePending?: boolean;
}

/** Skad pochodzi punkt (TreeLocationSource w backendzie); null/brak = punkt sprzed etapu 25. */
export type TreeLocationSource = 'MANUAL' | 'MAP' | 'DEVICE_GNSS' | 'IMPORT';

/**
 * TreeLocationResponse - punkt drzewa w stopniach dziesietnych (WGS84), zawsze kompletna para, oraz (etap 25)
 * opcjonalne dane pomiaru: wysokosc n.p.m. i dokladnosc pozioma w metrach, zrodlo punktu.
 */
export interface TreeLocation {
  latitude: number;
  longitude: number;
  altitudeM?: number | null;
  accuracyM?: number | null;
  source?: TreeLocationSource | null;
  /** etap 35: chwila pomiaru wg urzadzenia (ISO UTC), sposob pomiaru (kod adaptera GNSS), opis urzadzenia */
  measuredAt?: string | null;
  adapterCode?: string | null;
  deviceLabel?: string | null;
}

/**
 * Body PUT /api/projects/{projectId}/trees/{treeId}/location: punkt + opcjonalne dane pomiaru. Zrodlo IMPORT
 * ustawia tylko import plikow. Usuniecie lokalizacji to osobne DELETE, a nie wyslanie pustych pol.
 */
export interface TreeLocationRequest {
  latitude: number;
  longitude: number;
  altitudeM?: number;
  accuracyM?: number;
  source?: Exclude<TreeLocationSource, 'IMPORT'>;
  /** etap 35 - tylko z pomiaru GNSS (adapter); punkt z mapy albo wpisany recznie ich nie ma */
  measuredAt?: string;
  adapterCode?: string;
  deviceLabel?: string;
}

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
  /** etap 37: wersja danych, ktora widzial formularz - inna na serwerze = 409 (pominieta = zapis bez sprawdzania) */
  expectedDataVersion?: number;
}

/** Body POST /api/projects/{projectId}/trees. */
export type CreateTreeRequest = TreeEditableFieldsRequest;

/** Body PATCH /api/projects/{projectId}/trees/{treeId} - pominiete pole opcjonalne czysci wartosc na backendzie. */
export type UpdateTreeRequest = TreeEditableFieldsRequest;

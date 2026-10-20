// Oznaczenia drzew (etap 31) - odpowiadaja DTO backendu w pakiecie domain.treemarking.dto.

/** Oznaczenie z katalogu GET /api/tree-markings. */
export interface TreeMarkingDefinition {
  code: string;
  displayName: string;
  /** kod grupy, np. VALUE, MANAGEMENT */
  category: string;
  categoryDisplayName: string;
  /** 1-2 znaki rysowane na mapie */
  shortLabel: string;
  /** kolor kolka (#RRGGBB); znak bialy */
  color: string;
}

/** Oznaczenia jednego drzewa (kody w kolejnosci katalogu). */
export interface TreeMarkings {
  treeId: number;
  codes: string[];
  /** tylko frontend: zapisane na urzadzeniu, czeka na synchronizacje */
  offlinePending?: boolean;
}

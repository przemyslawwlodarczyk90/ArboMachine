// Typy modulu drzew - odpowiadaja DTO backendu w pakiecie domain.tree.dto.

/** TreeResponse. Daty jako napisy ISO-8601 w UTC (java.time.Instant). */
export interface Tree {
  id: number;
  projectId: number;
  /** uzytkowy identyfikator nadawany przez arboryste - tekst, np. "1", "A-12" */
  inventoryNumber: string;
  speciesLatin?: string | null;
  speciesPolish?: string | null;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * Pola drzewa ustawiane przez uzytkownika (TreeEditableFieldsRequest w backendzie) - bez projectId (idzie w URL)
 * i bez danych wlasciciela. Formularze zawsze wysylaja komplet pol; pominiete pole opcjonalne oznacza brak wartosci.
 */
export interface TreeEditableFieldsRequest {
  inventoryNumber: string;
  speciesLatin?: string;
  speciesPolish?: string;
  notes?: string;
}

/** Body POST /api/projects/{projectId}/trees. */
export type CreateTreeRequest = TreeEditableFieldsRequest;

/** Body PATCH /api/projects/{projectId}/trees/{treeId} - pominiete pole opcjonalne czysci wartosc na backendzie. */
export type UpdateTreeRequest = TreeEditableFieldsRequest;

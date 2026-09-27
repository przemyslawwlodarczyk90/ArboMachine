// Typy modulu drzew - odpowiadaja DTO backendu w pakiecie domain.tree.dto.

/** TreeResponse. Daty jako napisy ISO-8601 w UTC (java.time.Instant). */
export interface Tree {
  id: number;
  projectId: number;
  /** uzytkowy identyfikator nadawany przez arboryste - tekst, np. "1", "A-12" */
  inventoryNumber: string;
  speciesLatin?: string | null;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
}

/** CreateTreeRequest - bez projectId (idzie w URL) i bez danych wlasciciela. */
export interface CreateTreeRequest {
  inventoryNumber: string;
  speciesLatin?: string;
  notes?: string;
}

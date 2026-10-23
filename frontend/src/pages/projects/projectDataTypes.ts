// Typy modulu projektow - odpowiadaja DTO backendu w pakiecie domain.project.dto.

/** ProjectResponse. Daty jako napisy ISO-8601 w UTC (java.time.Instant). */
export interface Project {
  id: number;
  name: string;
  description?: string | null;
  /** etap 29: dane zlecenia (opcjonalne, wolny tekst) */
  clientName?: string | null;
  siteAddress?: string | null;
  /** 'YYYY-MM-DD' */
  orderDate?: string | null;
  /** etap 45: numer umowy / zlecenia i cel opracowania (do dokumentu PDF) */
  contractNumber?: string | null;
  documentPurpose?: string | null;
  createdAt: string;
  updatedAt: string;
}

/** CreateProjectRequest - celowo bez ownerId: wlasciciela ustala backend z tokenu JWT. */
export interface CreateProjectRequest {
  name: string;
  description?: string;
  clientName?: string;
  siteAddress?: string;
  orderDate?: string;
  contractNumber?: string;
  documentPurpose?: string;
}

/**
 * UpdateProjectRequest (PATCH) - pole pominiete = bez zmian. Formularz edycji zawsze wysyla oba pola;
 * pusty opis ("") czysci opis projektu.
 */
export interface UpdateProjectRequest {
  name?: string;
  description?: string;
  /** pusty tekst = usuniecie (etap 29) */
  clientName?: string;
  siteAddress?: string;
  orderDate?: string;
  /** etap 45: pusty tekst = usuniecie */
  contractNumber?: string;
  documentPurpose?: string;
}

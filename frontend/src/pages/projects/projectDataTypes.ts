// Typy modulu projektow - odpowiadaja DTO backendu w pakiecie domain.project.dto.

/** ProjectResponse. Daty jako napisy ISO-8601 w UTC (java.time.Instant). */
export interface Project {
  id: number;
  name: string;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
}

/** CreateProjectRequest - celowo bez ownerId: wlasciciela ustala backend z tokenu JWT. */
export interface CreateProjectRequest {
  name: string;
  description?: string;
}

/**
 * UpdateProjectRequest (PATCH) - pole pominiete = bez zmian. Formularz edycji zawsze wysyla oba pola;
 * pusty opis ("") czysci opis projektu.
 */
export interface UpdateProjectRequest {
  name?: string;
  description?: string;
}

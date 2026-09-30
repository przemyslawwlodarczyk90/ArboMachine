// Typy podsumowania projektu - odpowiadaja ProjectSummaryResponse (pakiet domain.projectsummary.dto).

/** Liczby projektu liczone w bazie. Zawsze treeWithLocationCount + treeWithoutLocationCount == treeCount. */
export interface ProjectSummary {
  treeCount: number;
  treeWithLocationCount: number;
  treeWithoutLocationCount: number;
  /** liczba zdjec (a nie liczba drzew ze zdjeciami) */
  photoCount: number;
  /** liczba zalecen/prac (a nie liczba drzew z pracami) */
  workCount: number;
}

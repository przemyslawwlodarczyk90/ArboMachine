// Waloryzacja drzewa (etap 27) - odpowiada TreeValuationResponse / TreeValuationRequest (domain.treevaluation).
// Modul bez importow wartosci - testowany przez node --test.

/**
 * Ogolna kategoria V1 - TO NIE JEST POTWIERDZONA SKALA BRANZOWA, tylko tymczasowa klasyfikacja wybierana recznie
 * przez arboryste (filtr, mapa, dokument). Brak kategorii = "Nie oceniono".
 */
export type TreeValuationCategory = 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH';

export const VALUATION_CATEGORY_OPTIONS: { category: TreeValuationCategory; label: string }[] = [
  { category: 'LOW', label: 'Niska' },
  { category: 'MEDIUM', label: 'Średnia' },
  { category: 'HIGH', label: 'Wysoka' },
  { category: 'VERY_HIGH', label: 'Bardzo wysoka' },
];

export const NOT_ASSESSED_LABEL = 'Nie oceniono';

export const valuationCategoryLabel = (category: TreeValuationCategory | null | undefined) =>
  VALUATION_CATEGORY_OPTIONS.find((option) => option.category === category)?.label ?? NOT_ASSESSED_LABEL;

/** Opisowe pola oceny - zapis slow arborysty, bez skal i punktow. */
export const VALUATION_TEXT_FIELDS = [
  { field: 'ageValue', label: 'Wartość wiekowa', placeholder: 'np. drzewo ok. 150-letnie, jedno z najstarszych w parku' },
  { field: 'naturalValue', label: 'Wartość przyrodnicza', placeholder: 'np. okazały egzemplarz rodzimego gatunku' },
  { field: 'ecologicalValue', label: 'Wartość ekologiczna', placeholder: 'np. dziuple - siedlisko ptaków i owadów' },
  { field: 'culturalValue', label: 'Wartość kulturowa', placeholder: 'np. element historycznego założenia parkowego' },
  { field: 'landscapeValue', label: 'Wartość krajobrazowa', placeholder: 'np. dominanta widoczna z głównej alei' },
  { field: 'justification', label: 'Uzasadnienie oceny', placeholder: 'Dlaczego taka ocena ogólna' },
  { field: 'notes', label: 'Uwagi', placeholder: '' },
] as const;

export type TreeValuationTextField = (typeof VALUATION_TEXT_FIELDS)[number]['field'];

export interface TreeValuation {
  treeId: number;
  /** false = drzewo nie ma jeszcze waloryzacji (pozostale pola puste) */
  hasValuation: boolean;
  overallCategory?: TreeValuationCategory | null;
  ageValue?: string | null;
  naturalValue?: string | null;
  ecologicalValue?: string | null;
  culturalValue?: string | null;
  landscapeValue?: string | null;
  justification?: string | null;
  notes?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  /** tylko frontend (etap 26): zapisane na urzadzeniu, czeka na synchronizacje */
  offlinePending?: boolean;
}

/** Body PUT .../valuation - pominiete pole = brak wartosci. */
export type TreeValuationRequest = Partial<Record<TreeValuationTextField, string>> & {
  overallCategory?: TreeValuationCategory;
};

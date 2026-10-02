import type { TreeValuationCategory } from '../valuation/valuationDataTypes.ts';
// Tryb "Gospodarka drzewostanem" (etap 28): legenda i kolory w works/workManagement.ts.

// Tryby widoku mapy projektu: kazdy tryb inaczej rysuje te same punkty drzew. Symbole prac (etap 19) i kolor
// waloryzacji (etap 27) nigdy nie sa mieszane w jednym trybie. Modul bez React - testowany przez node --test.

export type MapViewMode = 'SYMBOLS' | 'VALUATION' | 'MANAGEMENT';

export const MAP_VIEW_MODES: { mode: MapViewMode; label: string }[] = [
  { mode: 'SYMBOLS', label: 'Drzewa i symbole prac' },
  { mode: 'VALUATION', label: 'Waloryzacja' },
  { mode: 'MANAGEMENT', label: 'Gospodarka drzewostanem' },
];

/** Legenda trybu "Waloryzacja": wszystkie kategorie (takze z zerem) + "Nie oceniono", z liczba drzew na mapie. */
export function buildValuationLegend(points: { valuationCategory: TreeValuationCategory | null }[]):
  { category: TreeValuationCategory | null; count: number }[] {
  const categories: (TreeValuationCategory | null)[] = ['VERY_HIGH', 'HIGH', 'MEDIUM', 'LOW', null];
  return categories.map((category) => ({ category, count: points.filter((point) => point.valuationCategory === category).length }));
}

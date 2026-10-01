import type { TreeValuation, TreeValuationCategory } from './valuationDataTypes.ts';

// Filtr listy drzew po waloryzacji (etap 27). Modul bez React - testowany przez node --test.

/** ALL - wszystkie; NOT_ASSESSED - bez waloryzacji albo z waloryzacja bez oceny ogolnej; kategoria - tylko ta. */
export type ValuationFilter = 'ALL' | 'NOT_ASSESSED' | TreeValuationCategory;

/** Kategoria drzewa z listy waloryzacji projektu (null = nie oceniono). */
export function valuationCategoryByTreeId(valuations: TreeValuation[]): Map<number, TreeValuationCategory | null> {
  return new Map(valuations.map((valuation) => [valuation.treeId, valuation.overallCategory ?? null]));
}

export function filterTreesByValuation<T extends { id: number }>(trees: T[], categoryByTreeId: Map<number, TreeValuationCategory | null>,
                                                                 filter: ValuationFilter): T[] {
  if (filter === 'ALL') return trees;
  return trees.filter((tree) => {
    const category = categoryByTreeId.get(tree.id) ?? null;
    return filter === 'NOT_ASSESSED' ? category === null : category === filter;
  });
}

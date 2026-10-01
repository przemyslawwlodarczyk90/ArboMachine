import type { TreeValuationCategory } from './valuationDataTypes.ts';

// Kolory kategorii waloryzacji V1 (etap 27) - JEDNO miejsce dla odznak, mapy i legendy. To NIE jest standard
// branzowy: umowna skala od jasnej (niska) do ciemnej (bardzo wysoka), czytelna takze dla czesci osob z zaburzeniami
// widzenia barw (rozna jasnosc). Do uzgodnienia z arborysta (docs/DO_DOPRECYZOWANIA.md).

export const VALUATION_CATEGORY_COLORS: Record<TreeValuationCategory, { fill: string; text: string }> = {
  LOW: { fill: '#f2d16b', text: '#3d3000' },
  MEDIUM: { fill: '#8fcf6b', text: '#173d00' },
  HIGH: { fill: '#2f9e44', text: '#ffffff' },
  VERY_HIGH: { fill: '#14532d', text: '#ffffff' },
};

/** "Nie oceniono" - neutralny szary (inny niz zielony marker drzewa bez symbolu). */
export const NOT_ASSESSED_COLOR = { fill: '#9aa3a0', text: '#ffffff' };

export const valuationColorOf = (category: TreeValuationCategory | null | undefined) =>
  (category ? VALUATION_CATEGORY_COLORS[category] : NOT_ASSESSED_COLOR);

import type { MapSymbolShape } from './mapSymbolDataTypes';

// Opis geometrii symbolu niezalezny od OpenLayers (testowany przez node --test). mapSymbolStyleFactory zamienia go
// na styl OpenLayers, a MapSymbolSwatch - na miniature SVG, wiec mapa, legenda i wybor symbolu wygladaja tak samo.

export type MapSymbolShapeSpec =
  | { kind: 'circle' }
  /** wielokat foremny RegularShape: liczba wierzcholkow, obrot (radiany), radius2 = 0 -> "gwiazda" z samych linii */
  | { kind: 'polygon'; points: number; angle: number; radius2?: number; isStrokeOnly: boolean };

export function toMapSymbolShapeSpec(shape: MapSymbolShape | string): MapSymbolShapeSpec {
  switch (shape) {
    case 'SQUARE':
      return { kind: 'polygon', points: 4, angle: Math.PI / 4, isStrokeOnly: false };
    case 'TRIANGLE':
      return { kind: 'polygon', points: 3, angle: 0, isStrokeOnly: false };
    case 'DIAMOND':
      return { kind: 'polygon', points: 4, angle: 0, isStrokeOnly: false };
    case 'CROSS':
      return { kind: 'polygon', points: 4, angle: 0, radius2: 0, isStrokeOnly: true };
    case 'X':
      return { kind: 'polygon', points: 4, angle: Math.PI / 4, radius2: 0, isStrokeOnly: true };
    case 'CIRCLE':
    default:
      // Nieznany ksztalt (np. nowszy backend) - bezpieczny fallback do kola zamiast bledu mapy.
      return { kind: 'circle' };
  }
}

/** Maksymalna liczba znaczkow prac wokol drzewa; reszta jako "+N". Wartosc V1 (docs/DO_DOPRECYZOWANIA.md). */
export const MAXIMUM_VISIBLE_WORK_BADGES = 3;

/** Przesuniecia znaczkow prac w pikselach ekranu wzgledem punktu drzewa (OpenLayers: y rosnie w gore). */
const WORK_BADGE_DISPLACEMENTS: [number, number][] = [[13, 13], [-13, 13], [13, -13]];
const OVERFLOW_BADGE_DISPLACEMENT: [number, number] = [-13, -13];

export interface WorkBadgeLayout {
  badges: { symbolCode: string; displacement: [number, number] }[];
  /** ile symboli prac nie zmiescilo sie (pokazywane jako "+N"); 0 = brak znacznika */
  overflowCount: number;
  overflowDisplacement: [number, number];
}

/**
 * Rozmieszczenie znaczkow prac wokol drzewa - tylko wizualnie (displacement), wspolrzedne drzewa sie nie zmieniaja:
 * 1 praca - gora/prawo, 2 - gora/prawo + gora/lewo, 3 - dodatkowo dol/prawo, wiecej - "+N" dol/lewo.
 */
export function layoutWorkBadges(workSymbolCodes: string[]): WorkBadgeLayout {
  const visibleCodes = workSymbolCodes.slice(0, MAXIMUM_VISIBLE_WORK_BADGES);
  return {
    badges: visibleCodes.map((symbolCode, index) => ({ symbolCode, displacement: WORK_BADGE_DISPLACEMENTS[index] })),
    overflowCount: Math.max(0, workSymbolCodes.length - MAXIMUM_VISIBLE_WORK_BADGES),
    overflowDisplacement: OVERFLOW_BADGE_DISPLACEMENT,
  };
}

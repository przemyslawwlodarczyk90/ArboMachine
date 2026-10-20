// Rozmieszczenie oznaczen drzewa na mapie (etap 31): rzad kolek ze skrotami NAD punktem drzewa, wysrodkowany;
// powyzej limitu - "+N" na koncu rzedu. Tylko przesuniecia ekranowe (displacement, y w gore) - wspolrzedne drzewa
// sie nie zmieniaja. Modul bez OpenLayers - testowany przez node --test.

/** V1: tyle oznaczen widac przy drzewie (reszta jako "+N", pelna lista po kliknieciu). Do potwierdzenia z arborysta. */
export const MAXIMUM_VISIBLE_MARKINGS = 4;
/** Odstep srodkow kolek w rzedzie i wysokosc rzedu nad punktem (px). */
export const MARKING_BADGE_SPACING_PX = 18;
export const MARKING_ROW_HEIGHT_PX = 20;

export interface MarkingBadgeLayout {
  badges: { code: string; displacement: [number, number] }[];
  /** ile oznaczen sie nie zmiescilo; 0 = brak "+N" */
  overflowCount: number;
  overflowDisplacement: [number, number];
}

export function layoutMarkingBadges(codes: string[]): MarkingBadgeLayout {
  const visibleCodes = codes.slice(0, MAXIMUM_VISIBLE_MARKINGS);
  const overflowCount = Math.max(0, codes.length - MAXIMUM_VISIBLE_MARKINGS);
  // "+N" zajmuje jedno miejsce w rzedzie - rzad jest wysrodkowany razem z nim.
  const slotCount = visibleCodes.length + (overflowCount > 0 ? 1 : 0);
  const xOfSlot = (slot: number) => (slot - (slotCount - 1) / 2) * MARKING_BADGE_SPACING_PX;
  return {
    badges: visibleCodes.map((code, slot) => ({ code, displacement: [xOfSlot(slot), MARKING_ROW_HEIGHT_PX] })),
    overflowCount,
    overflowDisplacement: [xOfSlot(visibleCodes.length), MARKING_ROW_HEIGHT_PX],
  };
}

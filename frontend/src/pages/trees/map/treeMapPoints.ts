import type { Coordinate } from 'ol/coordinate.js';
// Rozszerzenia .ts: modul jest testowany bezposrednio w Node (node --test) - patrz treeLocationMapCoordinates.
import { toMapCoordinate } from '../location/treeLocationMapCoordinates.ts';
import { isTreeLocationWithinValidRange } from '../location/treeLocationValidRange.ts';
import type { Tree } from '../treeDataTypes';

/** Punkt drzewa na mapie projektu - to, co trafia do Feature OpenLayers. */
export interface TreeMapPoint {
  treeId: number;
  inventoryNumber: string;
  mapCoordinate: Coordinate;
  /** symbol drzewa (null = neutralny marker) */
  treeSymbolCode: string | null;
  /** symbole prac tego drzewa w kolejnosci prac (tylko prace z przypisanym symbolem) */
  workSymbolCodes: string[];
}

/** Minimum danych pracy potrzebne mapie (TreeWork z GET /api/projects/{projectId}/works). */
export interface TreeMapWorkInput {
  treeId: number;
  mapSymbolCode?: string | null;
}

/**
 * Drzewa projektu + prace projektu -> punkty mapy (laczenie po treeId w pamieci, bez zapytan o kazde drzewo).
 * Drzewo bez lokalizacji (albo z punktem spoza zakresu WGS84) po prostu nie ma markera - nadal istnieje
 * i jest widoczne na liscie drzew.
 */
export const toTreeMapPoints = (trees: Tree[], works: TreeMapWorkInput[] = []): TreeMapPoint[] => {
  const workSymbolCodesByTreeId = new Map<number, string[]>();
  for (const work of works) {
    if (!work.mapSymbolCode) continue;
    const treeWorkSymbolCodes = workSymbolCodesByTreeId.get(work.treeId) ?? [];
    treeWorkSymbolCodes.push(work.mapSymbolCode);
    workSymbolCodesByTreeId.set(work.treeId, treeWorkSymbolCodes);
  }
  return trees.flatMap((tree) => tree.location && isTreeLocationWithinValidRange(tree.location)
    ? [{
      treeId: tree.id,
      inventoryNumber: tree.inventoryNumber,
      mapCoordinate: toMapCoordinate(tree.location),
      treeSymbolCode: tree.mapSymbolCode ?? null,
      workSymbolCodes: workSymbolCodesByTreeId.get(tree.id) ?? [],
    }]
    : []);
};

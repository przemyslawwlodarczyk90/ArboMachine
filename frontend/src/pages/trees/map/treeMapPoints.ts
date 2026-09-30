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
}

/**
 * Drzewa projektu -> punkty mapy. Drzewo bez lokalizacji (albo z punktem spoza zakresu WGS84) po prostu
 * nie ma markera - nadal istnieje i jest widoczne na liscie drzew.
 */
export const toTreeMapPoints = (trees: Tree[]): TreeMapPoint[] =>
  trees.flatMap((tree) => tree.location && isTreeLocationWithinValidRange(tree.location)
    ? [{ treeId: tree.id, inventoryNumber: tree.inventoryNumber, mapCoordinate: toMapCoordinate(tree.location) }]
    : []);

import type { Coordinate } from 'ol/coordinate.js';
// Rozszerzenia .ts: modul jest testowany bezposrednio w Node (node --test) - patrz treeLocationMapCoordinates.
import { toMapCoordinate } from '../location/treeLocationMapCoordinates.ts';
import { isTreeLocationWithinValidRange } from '../location/treeLocationValidRange.ts';
import type { Tree } from '../treeDataTypes';
import type { TreeValuationCategory } from '../valuation/valuationDataTypes';
import type { TreeWorkPriority, TreeWorkStatus } from '../works/treeWorkDataTypes';
import {
  isOpenWorkStatus, managementSummaryByTreeId, NO_WORKS_SUMMARY, type TreeManagementSummary,
} from '../works/workManagement.ts';

/** Punkt drzewa na mapie projektu - to, co trafia do Feature OpenLayers. */
export interface TreeMapPoint {
  treeId: number;
  inventoryNumber: string;
  mapCoordinate: Coordinate;
  /** symbol drzewa (null = neutralny marker) */
  treeSymbolCode: string | null;
  /** symbole prac tego drzewa w kolejnosci prac (tylko prace z przypisanym symbolem) */
  workSymbolCodes: string[];
  /** kategoria waloryzacji V1 (etap 27); null = nie oceniono */
  valuationCategory: TreeValuationCategory | null;
  /** etap 28: symbole tylko prac do wykonania (tryb "Gospodarka drzewostanem") */
  openWorkSymbolCodes: string[];
  /** etap 28: stan prac drzewa (kolor markera w trybie "Gospodarka drzewostanem") */
  management: TreeManagementSummary;
}

/** Minimum danych pracy potrzebne mapie (TreeWork z GET /api/projects/{projectId}/works). */
export interface TreeMapWorkInput {
  treeId: number;
  mapSymbolCode?: string | null;
  /** etap 28; brak = zalecenie (do wykonania) */
  status?: TreeWorkStatus | null;
  priority?: TreeWorkPriority | null;
}

/**
 * Drzewa projektu + prace projektu -> punkty mapy (laczenie po treeId w pamieci, bez zapytan o kazde drzewo).
 * Drzewo bez lokalizacji (albo z punktem spoza zakresu WGS84) po prostu nie ma markera - nadal istnieje
 * i jest widoczne na liscie drzew.
 */
export const toTreeMapPoints = (trees: Tree[], works: TreeMapWorkInput[] = [],
                                valuationCategoryByTreeId: Map<number, TreeValuationCategory | null> = new Map()): TreeMapPoint[] => {
  const workSymbolCodesByTreeId = new Map<number, string[]>();
  const openWorkSymbolCodesByTreeId = new Map<number, string[]>();
  const append = (codesByTreeId: Map<number, string[]>, treeId: number, code: string) =>
    codesByTreeId.set(treeId, [...(codesByTreeId.get(treeId) ?? []), code]);
  for (const work of works) {
    if (!work.mapSymbolCode) continue;
    append(workSymbolCodesByTreeId, work.treeId, work.mapSymbolCode);
    if (isOpenWorkStatus(work.status)) append(openWorkSymbolCodesByTreeId, work.treeId, work.mapSymbolCode);
  }
  const managementByTreeId = managementSummaryByTreeId(works.map((work) => ({
    treeId: work.treeId, status: work.status ?? 'RECOMMENDED', priority: work.priority ?? null,
  })));
  return trees.flatMap((tree) => tree.location && isTreeLocationWithinValidRange(tree.location)
    ? [{
      treeId: tree.id,
      inventoryNumber: tree.inventoryNumber,
      mapCoordinate: toMapCoordinate(tree.location),
      treeSymbolCode: tree.mapSymbolCode ?? null,
      workSymbolCodes: workSymbolCodesByTreeId.get(tree.id) ?? [],
      valuationCategory: valuationCategoryByTreeId.get(tree.id) ?? null,
      openWorkSymbolCodes: openWorkSymbolCodesByTreeId.get(tree.id) ?? [],
      management: managementByTreeId.get(tree.id) ?? NO_WORKS_SUMMARY,
    }]
    : []);
};

import type { MapSymbol } from './mapSymbolDataTypes';

// Legenda mapy projektu wyliczana z danych (nie zapisywana w bazie). Modul bez importow wartosci - node --test.

export interface LegendTreeInput {
  id: number;
  hasLocation: boolean;
  mapSymbolCode?: string | null;
}

export interface LegendWorkInput {
  treeId: number;
  name: string;
  mapSymbolCode?: string | null;
}

export interface ProjectMapLegendEntry {
  symbol: MapSymbol;
  label: string;
  kind: 'tree' | 'work';
}

/**
 * Wpisy legendy z aktualnie widocznych danych: symbole drzew (z lokalizacja) oraz kombinacje "nazwa pracy + symbol"
 * dla prac tych drzew. Bez duplikatow. Tekst dla pracy pochodzi z TreeWork.name - nie zakladamy, ze np. RED_CROSS
 * oznacza wycinke. Kody spoza katalogu sa pomijane (nie ma czego narysowac).
 */
export function buildProjectMapLegend(trees: LegendTreeInput[], works: LegendWorkInput[],
                                      symbolByCode: Map<string, MapSymbol>): ProjectMapLegendEntry[] {
  const visibleTreeIds = new Set(trees.filter((tree) => tree.hasLocation).map((tree) => tree.id));
  const entries: ProjectMapLegendEntry[] = [];
  const seenKeys = new Set<string>();
  const addEntry = (kind: 'tree' | 'work', symbolCode: string | null | undefined, label: string) => {
    const symbol = symbolCode ? symbolByCode.get(symbolCode) : undefined;
    const key = `${kind}|${symbolCode}|${label.trim().toLocaleLowerCase('pl-PL')}`;
    if (!symbol || seenKeys.has(key)) return;
    seenKeys.add(key);
    entries.push({ symbol, label, kind });
  };
  for (const tree of trees) {
    if (visibleTreeIds.has(tree.id)) addEntry('tree', tree.mapSymbolCode, `Symbol drzewa — ${symbolByCode.get(tree.mapSymbolCode ?? '')?.displayName ?? ''}`);
  }
  for (const work of works) {
    if (visibleTreeIds.has(work.treeId)) addEntry('work', work.mapSymbolCode, work.name.trim());
  }
  return entries;
}

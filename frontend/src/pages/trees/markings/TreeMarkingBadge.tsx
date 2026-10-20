import type { TreeMarkingDefinition } from './treeMarkingDataTypes';

/** Oznaczenie jak na mapie: kolorowe kolko ze skrotem; withName - z nazwa obok (lista, legenda, karta drzewa). */
export function TreeMarkingBadge({ marking, withName = false }: { marking: TreeMarkingDefinition; withName?: boolean }) {
  return (
    <span className="tree-marking-badge-row" title={withName ? undefined : marking.displayName}>
      <span className="tree-marking-badge" style={{ background: marking.color }} aria-hidden={withName}>
        {marking.shortLabel}
      </span>
      {withName ? <span>{marking.displayName}</span> : <span className="visually-hidden">{marking.displayName}</span>}
    </span>
  );
}

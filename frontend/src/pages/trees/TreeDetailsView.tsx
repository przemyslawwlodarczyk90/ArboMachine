import { formatTimestampForDisplay } from '../../formatting/formatTimestampForDisplay';
import type { Tree } from './treeDataTypes';

/** Tryb podgladu karty drzewa - tylko wyswietla zapisane dane i przycisk przejscia do edycji. */
export function TreeDetailsView({ tree, startEditing }: { tree: Tree; startEditing: () => void }) {
  return (
    <>
      <div className="section-header-row">
        <h2>Drzewo {tree.inventoryNumber}</h2>
        <button className="btn btn-secondary" onClick={startEditing}>Edytuj</button>
      </div>
      <dl className="tree-details">
        <dt>Gatunek (nazwa łacińska)</dt>
        <dd>{tree.speciesLatin || <span className="muted">Nie podano</span>}</dd>
        <dt>Gatunek (nazwa polska)</dt>
        <dd>{tree.speciesPolish || <span className="muted">Nie podano</span>}</dd>
        <dt>Obwód pnia</dt>
        <dd>{tree.trunkCircumferenceCm != null ? `${tree.trunkCircumferenceCm} cm` : <span className="muted">Nie podano</span>}</dd>
        <dt>Wysokość</dt>
        <dd>{tree.heightM != null ? `${tree.heightM} m` : <span className="muted">Nie podano</span>}</dd>
        <dt>Średnica korony</dt>
        <dd>{tree.crownDiameterM != null ? `${tree.crownDiameterM} m` : <span className="muted">Nie podano</span>}</dd>
        <dt>Uwagi</dt>
        <dd className="tree-details-notes">{tree.notes || <span className="muted">Nie podano</span>}</dd>
        <dt>Utworzono</dt>
        <dd>{formatTimestampForDisplay(tree.createdAt)}</dd>
        <dt>Zaktualizowano</dt>
        <dd>{formatTimestampForDisplay(tree.updatedAt)}</dd>
      </dl>
    </>
  );
}

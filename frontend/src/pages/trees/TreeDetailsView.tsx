import { formatTimestampForDisplay } from '../../formatting/formatTimestampForDisplay';
import type { Tree } from './treeDataTypes';

/** Tryb podgladu karty drzewa - tylko wyswietla zapisane dane i przycisk przejscia do edycji. */
export function TreeDetailsView({ tree, startEditing }: { tree: Tree; startEditing: () => void }) {
  return (
    <>
      <div className="section-header-row">
        <h2>Tree {tree.inventoryNumber}</h2>
        <button className="btn btn-secondary" onClick={startEditing}>Edit</button>
      </div>
      <dl className="tree-details">
        <dt>Latin species</dt>
        <dd>{tree.speciesLatin || <span className="muted">Not provided</span>}</dd>
        <dt>Polish species</dt>
        <dd>{tree.speciesPolish || <span className="muted">Not provided</span>}</dd>
        <dt>Trunk circumference</dt>
        <dd>{tree.trunkCircumferenceCm != null ? `${tree.trunkCircumferenceCm} cm` : <span className="muted">Not provided</span>}</dd>
        <dt>Height</dt>
        <dd>{tree.heightM != null ? `${tree.heightM} m` : <span className="muted">Not provided</span>}</dd>
        <dt>Notes</dt>
        <dd className="tree-details-notes">{tree.notes || <span className="muted">Not provided</span>}</dd>
        <dt>Created</dt>
        <dd>{formatTimestampForDisplay(tree.createdAt)}</dd>
        <dt>Updated</dt>
        <dd>{formatTimestampForDisplay(tree.updatedAt)}</dd>
      </dl>
    </>
  );
}

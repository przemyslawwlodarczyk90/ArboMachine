import { useRef, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { ModalDialog } from '../../../components/ModalDialog';
import type { Tree } from '../treeDataTypes';
import { treesApi } from '../treesApi';
import { formatTreeLocationForDisplay } from './formatTreeLocationForDisplay';
import { TreeLocationForm } from './TreeLocationForm';

/**
 * Sekcja "Location" karty drzewa: pokazuje punkt albo "nie ustawiono" i pozwala go ustawic, zmienic lub usunac.
 * Zmienione drzewo oddaje wyzej (onTreeChanged) - TreePage trzyma jedno zrodlo prawdy o drzewie.
 * Przyszla mapa (TreeLocationMap) moze dojsc tutaj jako kolejny sposob ustawienia punktu obok formularza.
 */
export function TreeLocationSection({ projectId, tree, onTreeChanged }: {
  projectId: number;
  tree: Tree;
  onTreeChanged: (changedTree: Tree) => void;
}) {
  const [isEditingLocation, setIsEditingLocation] = useState(false);
  const [isRemovalDialogOpen, setIsRemovalDialogOpen] = useState(false);
  const [isRemovingLocation, setIsRemovingLocation] = useState(false);
  const [removalErrorMessage, setRemovalErrorMessage] = useState<string | null>(null);
  // Synchroniczna blokada podwojnego klikniecia "Remove".
  const isRemovalInProgress = useRef(false);

  const replaceTreeAndStopEditing = (savedTree: Tree) => {
    onTreeChanged(savedTree);
    setIsEditingLocation(false);
  };

  const removeLocation = async () => {
    if (isRemovalInProgress.current) return;
    isRemovalInProgress.current = true;
    setIsRemovingLocation(true);
    setRemovalErrorMessage(null);
    try {
      onTreeChanged(await treesApi.clearTreeLocation(projectId, tree.id));
      setIsRemovalDialogOpen(false);
    } catch (removalError) {
      setRemovalErrorMessage(removalError instanceof ApiRequestError ? removalError.message : 'Unexpected error.');
    } finally {
      isRemovalInProgress.current = false;
      setIsRemovingLocation(false);
    }
  };

  const openRemovalDialog = () => {
    setRemovalErrorMessage(null);
    setIsRemovalDialogOpen(true);
  };

  return (
    <section className="tree-location-section" aria-labelledby="tree-location-heading">
      <div className="section-header-row">
        <h3 id="tree-location-heading">Location</h3>
        {!isEditingLocation && (
          <div className="form-actions">
            {tree.location ? (
              <>
                <button className="btn btn-secondary" onClick={() => setIsEditingLocation(true)}>Change</button>
                <button className="btn btn-danger-outline" onClick={openRemovalDialog}>Remove</button>
              </>
            ) : (
              <button className="btn btn-primary" onClick={() => setIsEditingLocation(true)}>Set location</button>
            )}
          </div>
        )}
      </div>

      {isEditingLocation ? (
        <TreeLocationForm projectId={projectId} tree={tree} onTreeLocationSaved={replaceTreeAndStopEditing}
                          cancelEditing={() => setIsEditingLocation(false)} />
      ) : tree.location ? (
        <p className="tree-location-coordinates">{formatTreeLocationForDisplay(tree.location)}</p>
      ) : (
        <p className="muted">The location of this tree has not been set yet.</p>
      )}

      {isRemovalDialogOpen && (
        <ModalDialog title="Remove location?" closeDialog={() => setIsRemovalDialogOpen(false)}
                     isClosingBlocked={isRemovingLocation}>
          <div className="form">
            {removalErrorMessage && <Alert variant="error">{removalErrorMessage}</Alert>}
            <p className="muted">The tree will be marked as having no location. You can set it again at any time.</p>
            <div className="form-actions">
              <button type="button" className="btn btn-ghost" onClick={() => setIsRemovalDialogOpen(false)}
                      disabled={isRemovingLocation}>Cancel</button>
              <button type="button" className="btn btn-danger" onClick={removeLocation} disabled={isRemovingLocation}>
                {isRemovingLocation ? 'Removing…' : 'Remove location'}
              </button>
            </div>
          </div>
        </ModalDialog>
      )}
    </section>
  );
}

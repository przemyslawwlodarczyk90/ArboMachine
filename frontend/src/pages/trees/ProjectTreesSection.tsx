import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { parsePositiveIdRouteParam } from '../../routing/parsePositiveIdRouteParam';
import { NewTreeDialog } from './NewTreeDialog';
import type { Tree } from './treeDataTypes';
import { treesApi } from './treesApi';

/** "loading" i "empty" sa rozdzielone, zeby przed koncem zapytania nie mignal pusty stan. */
type TreeListState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; trees: Tree[] };

/**
 * Sekcja "Drzewa" workspace projektu (/projects/:projectId/trees). Renderuje sie wewnatrz ProjectPage dopiero
 * po udanym pobraniu projektu, wiec nie pobiera projektu ponownie - bierze tylko projectId z trasy.
 */
export function ProjectTreesSection() {
  const projectId = parsePositiveIdRouteParam(useParams().projectId);
  const [treeListState, setTreeListState] = useState<TreeListState>({ status: 'loading' });
  const [isNewTreeDialogOpen, setIsNewTreeDialogOpen] = useState(false);

  useEffect(() => {
    if (projectId === null) return;
    let isResponseStillWanted = true;
    setTreeListState({ status: 'loading' });
    treesApi.listTreesOfProject(projectId)
      .then((trees) => {
        if (isResponseStillWanted) setTreeListState({ status: 'loaded', trees });
      })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setTreeListState({
          status: 'failed',
          errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.',
        });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId]);

  if (projectId === null) return null; // ProjectPage pokazuje juz stan "nie istnieje"

  const appendCreatedTreeAndCloseDialog = (createdTree: Tree) => {
    setTreeListState((currentState) => currentState.status === 'loaded'
      ? { status: 'loaded', trees: [...currentState.trees, createdTree] }
      : { status: 'loaded', trees: [createdTree] });
    setIsNewTreeDialogOpen(false);
  };
  const openNewTreeDialog = () => setIsNewTreeDialogOpen(true);

  return (
    <div className="stack">
      <div className="section-header-row">
        <h2>Drzewa</h2>
        <button className="btn btn-primary" onClick={openNewTreeDialog}>Dodaj drzewo</button>
      </div>

      {treeListState.status === 'loading' && <div className="center"><LoadingSpinner /></div>}

      {treeListState.status === 'failed' && (
        <Alert variant="error">Nie udało się wczytać drzew projektu. {treeListState.errorMessage}</Alert>
      )}

      {treeListState.status === 'loaded' && treeListState.trees.length === 0 && (
        <div className="empty-state">
          <p>Do tego projektu nie dodano jeszcze drzew.</p>
          <button className="btn btn-primary" onClick={openNewTreeDialog}>Dodaj pierwsze drzewo</button>
        </div>
      )}

      {treeListState.status === 'loaded' && treeListState.trees.length > 0 && (
        <ul className="tree-list">
          {treeListState.trees.map((tree) => (
            <li key={tree.id}>
              <Link to={`/projects/${projectId}/trees/${tree.id}`} className="tree-list-item">
                <span className="tree-list-item-number">Drzewo {tree.inventoryNumber}</span>
                {tree.speciesLatin && <span className="tree-list-item-species">{tree.speciesLatin}</span>}
                {!tree.speciesLatin && tree.speciesPolish && <span>{tree.speciesPolish}</span>}
                {tree.notes && <span className="tree-list-item-notes">{tree.notes}</span>}
                {/* Pokazujemy tylko brak - to drzewa, ktore wymagaja jeszcze ustalenia punktu. */}
                {!tree.location && <span className="tree-list-item-no-location">Brak lokalizacji</span>}
              </Link>
            </li>
          ))}
        </ul>
      )}

      {isNewTreeDialogOpen && (
        <NewTreeDialog projectId={projectId} onTreeCreated={appendCreatedTreeAndCloseDialog}
                       closeDialog={() => setIsNewTreeDialogOpen(false)} />
      )}
    </div>
  );
}

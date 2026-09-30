import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { parsePositiveIdRouteParam } from '../../../routing/parsePositiveIdRouteParam';
import type { Tree } from '../treeDataTypes';
import { treesApi } from '../treesApi';
import { ProjectTreeMap } from './ProjectTreeMap';
import { toTreeMapPoints } from './treeMapPoints';

type TreeListState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; trees: Tree[] };

/**
 * Sekcja "Mapa" workspace projektu (/projects/:projectId/map) - podglad drzew z zapisana lokalizacja.
 * Korzysta z tej samej listy drzew co sekcja "Drzewa" (GET .../trees zawiera juz location) - bez zapytan
 * o lokalizacje pojedynczych drzew i bez osobnego endpointu mapy. Edycja punktu jest w karcie drzewa.
 * Renderuje sie dopiero po udanym pobraniu projektu (ProjectPage), wiec cudzy projekt tu nie dotrze.
 */
export function ProjectTreeMapSection() {
  const projectId = parsePositiveIdRouteParam(useParams().projectId);
  const [treeListState, setTreeListState] = useState<TreeListState>({ status: 'loading' });
  const [selectedTreeId, setSelectedTreeId] = useState<number | null>(null);

  useEffect(() => {
    if (projectId === null) return;
    let isResponseStillWanted = true;
    setTreeListState({ status: 'loading' });
    setSelectedTreeId(null);
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

  const trees = treeListState.status === 'loaded' ? treeListState.trees : null;
  // Stabilna tablica punktow - mapa dopasowuje widok tylko wtedy, gdy zmienia sie lista drzew.
  const treeMapPoints = useMemo(() => (trees ? toTreeMapPoints(trees) : []), [trees]);

  if (projectId === null) return null; // ProjectPage pokazuje juz stan "nie istnieje"

  if (treeListState.status === 'loading') {
    return <div className="center"><LoadingSpinner /></div>;
  }

  if (treeListState.status === 'failed') {
    return (
      <div className="stack">
        <h2>Mapa</h2>
        <Alert variant="error">Nie udało się wczytać drzew projektu. {treeListState.errorMessage}</Alert>
      </div>
    );
  }

  const treeCount = treeListState.trees.length;
  const treesWithoutLocationCount = treeCount - treeMapPoints.length;
  const selectedTree = treeListState.trees.find((tree) => tree.id === selectedTreeId) ?? null;

  return (
    <div className="stack">
      <div className="section-header-row">
        <h2>Mapa</h2>
        {treeCount > 0 && (
          <span className="muted small">Drzewa na mapie: {treeMapPoints.length} z {treeCount}</span>
        )}
      </div>

      {treeCount === 0 && (
        <Alert variant="info">
          W tym projekcie nie ma jeszcze drzew. <Link to={`/projects/${projectId}/trees`}>Przejdź do listy drzew</Link>
        </Alert>
      )}
      {treeCount > 0 && treeMapPoints.length === 0 && (
        <Alert variant="info">Brak drzew z zapisaną lokalizacją.</Alert>
      )}
      {treeMapPoints.length > 0 && treesWithoutLocationCount > 0 && (
        <p className="muted small">
          Drzewa bez lokalizacji: {treesWithoutLocationCount} (niewidoczne na mapie). Lokalizację ustawisz w karcie
          drzewa.
        </p>
      )}

      <div className="map-frame">
        <ProjectTreeMap treeMapPoints={treeMapPoints} onTreeMarkerClicked={setSelectedTreeId} />
        {selectedTree && (
          <div className="map-selected-tree card" role="status">
            <div className="map-selected-tree-text">
              <strong>Drzewo {selectedTree.inventoryNumber}</strong>
              {(selectedTree.speciesPolish || selectedTree.speciesLatin) && (
                <span className="muted small">
                  {[selectedTree.speciesPolish, selectedTree.speciesLatin].filter(Boolean).join(' · ')}
                </span>
              )}
            </div>
            <div className="form-actions">
              <button type="button" className="btn btn-ghost" onClick={() => setSelectedTreeId(null)}>Zamknij</button>
              <Link to={`/projects/${projectId}/trees/${selectedTree.id}`} className="btn btn-primary">
                Przejdź do karty drzewa
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

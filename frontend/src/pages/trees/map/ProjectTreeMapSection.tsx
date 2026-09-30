import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { buildProjectMapLegend } from '../../../map/mapSymbolLegend';
import { MapSymbolSwatch } from '../../../map/MapSymbolSwatch';
import { useMapSymbolCatalog } from '../../../map/useMapSymbolCatalog';
import { parsePositiveIdRouteParam } from '../../../routing/parsePositiveIdRouteParam';
import type { Tree } from '../treeDataTypes';
import { treesApi } from '../treesApi';
import type { TreeWork } from '../works/treeWorkDataTypes';
import { treeWorksApi } from '../works/treeWorksApi';
import { ProjectMapLegend } from './ProjectMapLegend';
import { ProjectTreeMap } from './ProjectTreeMap';
import { toTreeMapPoints } from './treeMapPoints';

type ProjectMapDataState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; trees: Tree[]; works: TreeWork[] };

/**
 * Sekcja "Mapa" workspace projektu (/projects/:projectId/map) - podglad drzew z lokalizacja, ich symboli i symboli prac.
 * Dane: GET .../trees, GET .../works (wszystkie prace projektu jednym zapytaniem) i katalog GET /api/map-symbols,
 * laczone w pamieci po treeId - bez zapytan o kazde drzewo. Edycja punktu i symboli jest w karcie drzewa.
 * Renderuje sie dopiero po udanym pobraniu projektu (ProjectPage), wiec cudzy projekt tu nie dotrze.
 */
export function ProjectTreeMapSection() {
  const projectId = parsePositiveIdRouteParam(useParams().projectId);
  const [projectMapDataState, setProjectMapDataState] = useState<ProjectMapDataState>({ status: 'loading' });
  const [selectedTreeId, setSelectedTreeId] = useState<number | null>(null);
  const { catalogState, symbolByCode } = useMapSymbolCatalog();

  useEffect(() => {
    if (projectId === null) return;
    let isResponseStillWanted = true;
    setProjectMapDataState({ status: 'loading' });
    setSelectedTreeId(null);
    Promise.all([treesApi.listTreesOfProject(projectId), treeWorksApi.listProjectWorks(projectId)])
      .then(([trees, works]) => {
        if (isResponseStillWanted) setProjectMapDataState({ status: 'loaded', trees, works });
      })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setProjectMapDataState({
          status: 'failed',
          errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.',
        });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId]);

  const loadedData = projectMapDataState.status === 'loaded' ? projectMapDataState : null;
  // Stabilne tablice - mapa dopasowuje widok tylko wtedy, gdy zmieniaja sie dane projektu.
  const treeMapPoints = useMemo(
    () => (loadedData ? toTreeMapPoints(loadedData.trees, loadedData.works) : []), [loadedData]);
  const legendEntries = useMemo(() => (loadedData
    ? buildProjectMapLegend(
      loadedData.trees.map((tree) => ({ id: tree.id, hasLocation: !!tree.location, mapSymbolCode: tree.mapSymbolCode })),
      loadedData.works, symbolByCode)
    : []), [loadedData, symbolByCode]);

  if (projectId === null) return null; // ProjectPage pokazuje juz stan "nie istnieje"

  if (projectMapDataState.status === 'loading') {
    return <div className="center"><LoadingSpinner /></div>;
  }

  if (projectMapDataState.status === 'failed') {
    return (
      <div className="stack">
        <h2>Mapa</h2>
        <Alert variant="error">Nie udało się pobrać danych mapy. {projectMapDataState.errorMessage}</Alert>
      </div>
    );
  }

  const treeCount = projectMapDataState.trees.length;
  const treesWithoutLocationCount = treeCount - treeMapPoints.length;
  const selectedTree = projectMapDataState.trees.find((tree) => tree.id === selectedTreeId) ?? null;
  const selectedTreeWorks = projectMapDataState.works.filter((work) => work.treeId === selectedTreeId);
  const selectedTreeSymbol = selectedTree?.mapSymbolCode ? symbolByCode.get(selectedTree.mapSymbolCode) : undefined;

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
      {catalogState.status === 'failed' && (
        <Alert variant="error">
          Nie udało się pobrać katalogu symboli - drzewa są pokazane bez symboli. {catalogState.errorMessage}
        </Alert>
      )}

      <div className="map-frame">
        <ProjectTreeMap treeMapPoints={treeMapPoints} symbolByCode={symbolByCode} onTreeMarkerClicked={setSelectedTreeId} />
        {selectedTree && (
          <div className="map-selected-tree card" role="status">
            <div className="map-selected-tree-text">
              <strong>
                {selectedTreeSymbol && <MapSymbolSwatch symbol={selectedTreeSymbol} size={18} />} Drzewo {selectedTree.inventoryNumber}
              </strong>
              {(selectedTree.speciesPolish || selectedTree.speciesLatin) && (
                <span className="muted small">
                  {[selectedTree.speciesPolish, selectedTree.speciesLatin].filter(Boolean).join(' · ')}
                </span>
              )}
              {selectedTreeWorks.length > 0 && (
                <ul className="map-selected-tree-works" aria-label="Zalecenia i prace drzewa">
                  {selectedTreeWorks.map((work) => {
                    const workSymbol = work.mapSymbolCode ? symbolByCode.get(work.mapSymbolCode) : undefined;
                    return (
                      <li key={work.id}>
                        {workSymbol ? <MapSymbolSwatch symbol={workSymbol} size={16} /> : <span className="map-symbol-none" aria-hidden="true" />}
                        {' '}{work.name}
                      </li>
                    );
                  })}
                </ul>
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

      {treeMapPoints.length > 0 && <ProjectMapLegend entries={legendEntries} />}
    </div>
  );
}

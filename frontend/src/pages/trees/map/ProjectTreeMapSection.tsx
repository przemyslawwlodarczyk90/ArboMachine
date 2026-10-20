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
import { BasemapControls } from '../../../map/BasemapControls';
import type { TreeStand } from '../../projects/stands/standDataTypes';
import { treeStandsApi } from '../../projects/stands/treeStandsApi';
import { ProjectTreeMap } from './ProjectTreeMap';
import { toTreeMapPoints } from './treeMapPoints';
import { treeValuationsApi } from '../valuation/treeValuationsApi';
import { ValuationBadge } from '../valuation/ValuationBadge';
import { valuationColorOf } from '../valuation/valuationColors';
import { valuationCategoryLabel, type TreeValuation } from '../valuation/valuationDataTypes';
import { valuationCategoryByTreeId } from '../valuation/valuationFilter';
import { buildMarkingLegend, buildValuationLegend, MAP_VIEW_MODES, type MapViewMode } from './mapViewMode';
import { TreeMarkingBadge } from '../markings/TreeMarkingBadge';
import type { TreeMarkings } from '../markings/treeMarkingDataTypes';
import { treeMarkingsApi } from '../markings/treeMarkingsApi';
import { useTreeMarkingCatalog } from '../markings/useTreeMarkingCatalog';
import { TreeWorkMeta } from '../works/TreeWorkMeta';
import {
  buildManagementLegend, isOpenWorkStatus, managementColorOf, managementStateLabel, sortWorksForManagement,
} from '../works/workManagement';

/** Stala pusta lista - ukrycie warstwy nie tworzy nowej tablicy przy kazdym renderze. */
const NO_STANDS: TreeStand[] = [];

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
  const [viewMode, setViewMode] = useState<MapViewMode>('SYMBOLS');
  // Waloryzacje (etap 27) - osobne zapytanie; blad nie blokuje mapy (tryb "Waloryzacja" pokaze wtedy komunikat).
  const [valuations, setValuations] = useState<TreeValuation[] | null>(null);
  // Grupy drzew (etap 29): warstwa obszarow, domyslnie widoczna, gdy projekt je ma.
  const [stands, setStands] = useState<TreeStand[]>([]);
  const [areStandsVisible, setAreStandsVisible] = useState(true);
  // Oznaczenia drzew (etap 31) - osobne zapytanie; blad nie blokuje mapy. Filtr: kod oznaczenia albo '' = wszystkie.
  const [projectMarkings, setProjectMarkings] = useState<TreeMarkings[] | null>(null);
  const [markingFilterCode, setMarkingFilterCode] = useState('');
  const { catalogState: markingCatalogState, markingByCode } = useTreeMarkingCatalog();

  useEffect(() => {
    if (projectId === null) return;
    let isResponseStillWanted = true;
    setProjectMapDataState({ status: 'loading' });
    setSelectedTreeId(null);
    treeValuationsApi.listProjectValuations(projectId)
      .then((loadedValuations) => { if (isResponseStillWanted) setValuations(loadedValuations); })
      .catch(() => { if (isResponseStillWanted) setValuations(null); });
    treeMarkingsApi.listProjectMarkings(projectId)
      .then((loadedMarkings) => { if (isResponseStillWanted) setProjectMarkings(loadedMarkings); })
      .catch(() => { if (isResponseStillWanted) setProjectMarkings(null); });
    treeStandsApi.listStands(projectId)
      .then((loadedStands) => { if (isResponseStillWanted) setStands(loadedStands); })
      .catch(() => { if (isResponseStillWanted) setStands([]); });
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
  const allTreeMapPoints = useMemo(
    () => (loadedData ? toTreeMapPoints(loadedData.trees, loadedData.works, valuationCategoryByTreeId(valuations ?? []),
      new Map((projectMarkings ?? []).map((markings) => [markings.treeId, markings.codes]))) : []),
    [loadedData, valuations, projectMarkings]);
  // Filtr oznaczen dziala tylko w trybie "Oznaczenia drzew" - w innych trybach mapa pokazuje wszystkie drzewa.
  const treeMapPoints = useMemo(() => (viewMode === 'MARKINGS' && markingFilterCode
    ? allTreeMapPoints.filter((point) => point.markingCodes.includes(markingFilterCode))
    : allTreeMapPoints), [allTreeMapPoints, viewMode, markingFilterCode]);
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
  const treesWithoutLocationCount = treeCount - allTreeMapPoints.length;
  const markingCatalogCodes = markingCatalogState.status === 'loaded' ? markingCatalogState.markings.map((marking) => marking.code) : [];
  const selectedTreeMarkingCodes = allTreeMapPoints.find((point) => point.treeId === selectedTreeId)?.markingCodes ?? [];
  const selectedTree = projectMapDataState.trees.find((tree) => tree.id === selectedTreeId) ?? null;
  const allSelectedTreeWorks = projectMapDataState.works.filter((work) => work.treeId === selectedTreeId);
  // W trybie gospodarki tylko prace do wykonania, w kolejnosci terminow (jak w widoku "Gospodarka drzewostanem").
  const selectedTreeWorks = viewMode === 'MANAGEMENT'
    ? sortWorksForManagement(allSelectedTreeWorks.filter((work) => isOpenWorkStatus(work.status)))
    : allSelectedTreeWorks;
  const selectedTreeSymbol = selectedTree?.mapSymbolCode ? symbolByCode.get(selectedTree.mapSymbolCode) : undefined;
  const selectedTreeValuationCategory = treeMapPoints.find((point) => point.treeId === selectedTreeId)?.valuationCategory ?? null;

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
      {treeCount > 0 && allTreeMapPoints.length === 0 && (
        <Alert variant="info">Brak drzew z zapisaną lokalizacją.</Alert>
      )}
      {allTreeMapPoints.length > 0 && treesWithoutLocationCount > 0 && (
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

      {allTreeMapPoints.length > 0 && (
        <div className="map-view-mode" role="radiogroup" aria-label="Widok mapy">
          <span className="muted small">Widok:</span>
          {MAP_VIEW_MODES.map(({ mode, label }) => (
            <button key={mode} type="button" role="radio" aria-checked={viewMode === mode}
                    className={`btn btn-small ${viewMode === mode ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setViewMode(mode)}>
              {label}
            </button>
          ))}
        </div>
      )}
      {viewMode === 'MARKINGS' && (
        <div className="map-layer-toggles">
          <label className="muted small" htmlFor="map-marking-filter">Pokaż:</label>
          <select id="map-marking-filter" className="select" value={markingFilterCode}
                  onChange={(changeEvent) => setMarkingFilterCode(changeEvent.target.value)}>
            <option value="">wszystkie drzewa</option>
            {markingCatalogState.status === 'loaded' && markingCatalogState.markings.map((marking) => (
              <option key={marking.code} value={marking.code}>tylko: {marking.displayName}</option>
            ))}
          </select>
          {markingFilterCode && <span className="muted small">Drzewa z tym oznaczeniem: {treeMapPoints.length}</span>}
        </div>
      )}
      {viewMode === 'MARKINGS' && projectMarkings === null && (
        <Alert variant="error">Nie udało się pobrać oznaczeń drzew - drzewa są pokazane bez oznaczeń.</Alert>
      )}
      {(stands.length > 0 || allTreeMapPoints.length > 0) && (
        <div className="map-layer-toggles">
          {stands.length > 0 && (
            <label className="checkbox-row">
              <input type="checkbox" checked={areStandsVisible} onChange={(changeEvent) => setAreStandsVisible(changeEvent.target.checked)} />
              <span>Pokaż grupy drzew ({stands.length})</span>
            </label>
          )}
          <span className="muted small">
            <span className="map-legend-swatch-circle" style={{ background: '#2f6b3c' }} aria-hidden="true" /> liściaste / gatunek nie podany
            {' · '}<span className="map-legend-star" aria-hidden="true" /> iglaste
            {stands.length > 0 && <>{' · '}<span className="map-legend-stand" aria-hidden="true" /> grupa drzew</>}
          </span>
        </div>
      )}
      {viewMode === 'VALUATION' && valuations === null && (
        <Alert variant="error">Nie udało się pobrać waloryzacji - wszystkie drzewa są pokazane jako „Nie oceniono”.</Alert>
      )}

      <div className="map-frame">
        <ProjectTreeMap treeMapPoints={treeMapPoints} symbolByCode={symbolByCode} onTreeMarkerClicked={setSelectedTreeId}
                        viewMode={viewMode} stands={areStandsVisible ? stands : NO_STANDS} markingByCode={markingByCode} />
        {selectedTree && (
          <div className="map-selected-tree card" role="status">
            <div className="map-selected-tree-text">
              <strong>
                {selectedTreeSymbol && <MapSymbolSwatch symbol={selectedTreeSymbol} size={18} />} Drzewo {selectedTree.inventoryNumber}
              </strong>
              {viewMode === 'VALUATION' && <ValuationBadge category={selectedTreeValuationCategory} prefix="Waloryzacja: " />}
              {selectedTreeMarkingCodes.length > 0 && (
                <span className="tree-marking-list" aria-label="Oznaczenia drzewa">
                  {selectedTreeMarkingCodes.map((code) => markingByCode.get(code)).filter((marking) => marking !== undefined)
                    .map((marking) => <TreeMarkingBadge key={marking.code} marking={marking} withName />)}
                </span>
              )}
              {viewMode === 'MANAGEMENT' && selectedTreeWorks.length === 0 && (
                <span className="muted small">Brak prac do wykonania.</span>
              )}
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
                        {viewMode === 'MANAGEMENT' && <> <TreeWorkMeta treeWork={work} /></>}
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

      <BasemapControls />

      {treeMapPoints.length > 0 && viewMode === 'SYMBOLS' && <ProjectMapLegend entries={legendEntries} />}
      {allTreeMapPoints.length > 0 && viewMode === 'MARKINGS' && (
        <section className="map-legend" aria-labelledby="map-marking-legend-heading">
          <h3 id="map-marking-legend-heading">Legenda - oznaczenia drzew</h3>
          {buildMarkingLegend(allTreeMapPoints, markingCatalogCodes).length === 0 ? (
            <p className="muted small">Żadne drzewo na mapie nie ma jeszcze oznaczeń. Oznaczenia dodasz w karcie drzewa.</p>
          ) : (
            <ul>
              {buildMarkingLegend(allTreeMapPoints, markingCatalogCodes).map(({ code, count }) => (
                <li key={code}><TreeMarkingBadge marking={markingByCode.get(code)!} withName /> ({count})</li>
              ))}
            </ul>
          )}
          <p className="muted small">Przy drzewie widać do 4 oznaczeń, resztę jako „+N” - pełna lista po kliknięciu drzewa.</p>
        </section>
      )}
      {treeMapPoints.length > 0 && viewMode === 'VALUATION' && (
        <section className="map-legend" aria-labelledby="map-valuation-legend-heading">
          <h3 id="map-valuation-legend-heading">Legenda - waloryzacja</h3>
          <ul>
            {buildValuationLegend(treeMapPoints).map(({ category, count }) => (
              <li key={category ?? 'NONE'}>
                <span className="map-legend-swatch-circle" style={{ background: valuationColorOf(category).fill }} aria-hidden="true" />
                {' '}{valuationCategoryLabel(category)} ({count})
              </li>
            ))}
          </ul>
          <p className="muted small">
            Kategorie i kolory są tymczasowe (wersja V1) - to nie jest skala branżowa. Ocenę ogólną wybiera arborysta w karcie drzewa.
          </p>
        </section>
      )}
      {treeMapPoints.length > 0 && viewMode === 'MANAGEMENT' && (
        <section className="map-legend" aria-labelledby="map-management-legend-heading">
          <h3 id="map-management-legend-heading">Legenda - gospodarka drzewostanem</h3>
          <ul>
            {buildManagementLegend(treeMapPoints).map(({ state, count }) => (
              <li key={state}>
                <span className="map-legend-swatch-circle" style={{ background: managementColorOf(state) }} aria-hidden="true" />
                {' '}{managementStateLabel(state)} ({count})
              </li>
            ))}
          </ul>
          <p className="muted small">
            Prace do wykonania = zalecenia i prace zaplanowane. Przy drzewie widać symbole tylko tych prac. Pełna lista:{' '}
            <Link to={`/projects/${projectId}/management`}>Gospodarka drzewostanem</Link>.
          </p>
        </section>
      )}
    </div>
  );
}

import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { useOfflineState } from '../../offline/useOfflineState';
import { parsePositiveIdRouteParam } from '../../routing/parsePositiveIdRouteParam';
import { HealthStatusBadge } from './monitoring/HealthStatusBadge';
import { HEALTH_STATUS_OPTIONS, NOT_INSPECTED_LABEL, type TreeMonitoringRecord } from './monitoring/monitoringDataTypes';
import { filterTreesByMonitoring, latestRecordByTreeId, type MonitoringFilter } from './monitoring/monitoringFormValues';
import { treeMonitoringApi } from './monitoring/treeMonitoringApi';
import { NewTreeDialog } from './NewTreeDialog';
import { SPECIES_GROUP_LABELS, speciesGroupOf, type SpeciesGroup } from '../../map/speciesGroup';
import type { TreeStand } from '../projects/stands/standDataTypes';
import { treeStandsApi } from '../projects/stands/treeStandsApi';
import type { Tree } from './treeDataTypes';
import { treesApi } from './treesApi';
import { TreeMarkingBadge } from './markings/TreeMarkingBadge';
import type { TreeMarkings } from './markings/treeMarkingDataTypes';
import { treeMarkingsApi } from './markings/treeMarkingsApi';
import { useTreeMarkingCatalog } from './markings/useTreeMarkingCatalog';
import { treeValuationsApi } from './valuation/treeValuationsApi';
import { ValuationBadge } from './valuation/ValuationBadge';
import { VALUATION_CATEGORY_OPTIONS, type TreeValuation } from './valuation/valuationDataTypes';
import { filterTreesByValuation, valuationCategoryByTreeId, type ValuationFilter } from './valuation/valuationFilter';

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
  const { syncGeneration, pendingTreeIds } = useOfflineState();
  // Waloryzacje projektu jednym zapytaniem (etap 27); blad nie blokuje listy - drzewa bez odznak i bez filtra.
  const [valuations, setValuations] = useState<TreeValuation[] | null>(null);
  const [valuationFilter, setValuationFilter] = useState<ValuationFilter>('ALL');
  // Ostatnia kontrola stanu kazdego drzewa (etap 29); blad nie blokuje listy.
  const [latestRecords, setLatestRecords] = useState<TreeMonitoringRecord[] | null>(null);
  const [monitoringFilter, setMonitoringFilter] = useState<MonitoringFilter>('ALL');
  // Etap 29: grupy drzew i grupa gatunku (iglaste / lisciaste z nazwy gatunku).
  const [stands, setStands] = useState<TreeStand[]>([]);
  const [standFilter, setStandFilter] = useState<'ALL' | 'NONE' | number>('ALL');
  const [speciesGroupFilter, setSpeciesGroupFilter] = useState<'ALL' | 'UNKNOWN' | SpeciesGroup>('ALL');
  // Etap 31: oznaczenia drzew - plakietki w wierszu i filtr ('' = wszystkie, 'NONE' = bez oznaczen, kod = z oznaczeniem).
  const [projectMarkings, setProjectMarkings] = useState<TreeMarkings[] | null>(null);
  const [markingFilter, setMarkingFilter] = useState('');
  const { catalogState: markingCatalogState, markingByCode } = useTreeMarkingCatalog();

  useEffect(() => {
    if (projectId === null) return;
    let isResponseStillWanted = true;
    setTreeListState({ status: 'loading' });
    treeValuationsApi.listProjectValuations(projectId)
      .then((loadedValuations) => { if (isResponseStillWanted) setValuations(loadedValuations); })
      .catch(() => { if (isResponseStillWanted) setValuations(null); });
    treeMarkingsApi.listProjectMarkings(projectId)
      .then((loadedMarkings) => { if (isResponseStillWanted) setProjectMarkings(loadedMarkings); })
      .catch(() => { if (isResponseStillWanted) setProjectMarkings(null); });
    treeStandsApi.listStands(projectId)
      .then((loadedStands) => { if (isResponseStillWanted) setStands(loadedStands); })
      .catch(() => { if (isResponseStillWanted) setStands([]); });
    treeMonitoringApi.listLatestRecordsOfProject(projectId)
      .then((loadedRecords) => { if (isResponseStillWanted) setLatestRecords(loadedRecords); })
      .catch(() => { if (isResponseStillWanted) setLatestRecords(null); });
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
  }, [projectId, syncGeneration]);

  const categoryByTreeId = useMemo(() => valuationCategoryByTreeId(valuations ?? []), [valuations]);
  const latestByTreeId = useMemo(() => latestRecordByTreeId(latestRecords ?? []), [latestRecords]);
  const markingCodesByTreeId = useMemo(() => new Map((projectMarkings ?? []).map((markings) => [markings.treeId, markings.codes])),
    [projectMarkings]);

  if (projectId === null) return null; // ProjectPage pokazuje juz stan "nie istnieje"
  const treeIdsInAnyStand = new Set(stands.flatMap((stand) => stand.treeIds));
  const selectedStandTreeIds = typeof standFilter === 'number' ? new Set(stands.find((stand) => stand.id === standFilter)?.treeIds ?? []) : null;
  const visibleTrees = treeListState.status === 'loaded'
    ? filterTreesByMonitoring(filterTreesByValuation(treeListState.trees, categoryByTreeId, valuationFilter), latestByTreeId, monitoringFilter)
      .filter((tree) => standFilter === 'ALL' || (standFilter === 'NONE' ? !treeIdsInAnyStand.has(tree.id) : selectedStandTreeIds!.has(tree.id)))
      .filter((tree) => {
        if (speciesGroupFilter === 'ALL') return true;
        const group = speciesGroupOf(tree.speciesLatin, tree.speciesPolish);
        return speciesGroupFilter === 'UNKNOWN' ? group === null : group === speciesGroupFilter;
      })
      .filter((tree) => {
        if (!markingFilter) return true;
        const codes = markingCodesByTreeId.get(tree.id) ?? [];
        return markingFilter === 'NONE' ? codes.length === 0 : codes.includes(markingFilter);
      })
    : [];

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

      {treeListState.status === 'loaded' && treeListState.trees.length > 0 && (valuations !== null || latestRecords !== null) && (
        <div className="tree-list-filters">
          {valuations !== null && <div className="field">
            <label htmlFor="tree-valuation-filter">Waloryzacja</label>
            <select id="tree-valuation-filter" value={valuationFilter}
                    onChange={(changeEvent) => setValuationFilter(changeEvent.target.value as ValuationFilter)}>
              <option value="ALL">Wszystkie</option>
              <option value="NOT_ASSESSED">Nieocenione</option>
              {VALUATION_CATEGORY_OPTIONS.map(({ category, label }) => <option key={category} value={category}>{label}</option>)}
            </select>
          </div>}
          <div className="field">
            <label htmlFor="tree-species-group-filter">Iglaste / liściaste</label>
            <select id="tree-species-group-filter" value={speciesGroupFilter}
                    onChange={(changeEvent) => setSpeciesGroupFilter(changeEvent.target.value as typeof speciesGroupFilter)}>
              <option value="ALL">Wszystkie</option>
              <option value="CONIFER">{SPECIES_GROUP_LABELS.CONIFER}</option>
              <option value="DECIDUOUS">{SPECIES_GROUP_LABELS.DECIDUOUS}</option>
              <option value="UNKNOWN">Gatunek nie podany</option>
            </select>
          </div>
          {projectMarkings !== null && markingCatalogState.status === 'loaded' && (
            <div className="field">
              <label htmlFor="tree-marking-filter">Oznaczenie</label>
              <select id="tree-marking-filter" value={markingFilter} onChange={(changeEvent) => setMarkingFilter(changeEvent.target.value)}>
                <option value="">Wszystkie</option>
                <option value="NONE">Bez oznaczeń</option>
                {markingCatalogState.markings.map((marking) => <option key={marking.code} value={marking.code}>{marking.displayName}</option>)}
              </select>
            </div>
          )}
          {stands.length > 0 && (
            <div className="field">
              <label htmlFor="tree-stand-filter">Grupa drzew</label>
              <select id="tree-stand-filter" value={String(standFilter)}
                      onChange={(changeEvent) => setStandFilter(['ALL', 'NONE'].includes(changeEvent.target.value)
                        ? changeEvent.target.value as 'ALL' | 'NONE' : Number(changeEvent.target.value))}>
                <option value="ALL">Wszystkie</option>
                <option value="NONE">Poza grupami</option>
                {stands.map((stand) => <option key={stand.id} value={stand.id}>{stand.name}</option>)}
              </select>
            </div>
          )}
          {latestRecords !== null && (
            <div className="field">
              <label htmlFor="tree-monitoring-filter">Stan (ostatnia kontrola)</label>
              <select id="tree-monitoring-filter" value={monitoringFilter}
                      onChange={(changeEvent) => setMonitoringFilter(changeEvent.target.value as MonitoringFilter)}>
                <option value="ALL">Wszystkie</option>
                <option value="NOT_INSPECTED">{NOT_INSPECTED_LABEL}</option>
                {HEALTH_STATUS_OPTIONS.map(({ status, label }) => <option key={status} value={status}>{label}</option>)}
                <option value="NOT_SPECIFIED">Bez oceny</option>
              </select>
            </div>
          )}
          <span className="muted small">Pokazano {visibleTrees.length} z {treeListState.trees.length}</span>
        </div>
      )}

      {treeListState.status === 'loaded' && treeListState.trees.length > 0 && visibleTrees.length === 0 && (
        <p className="muted">Żadne drzewo nie pasuje do wybranych filtrów.</p>
      )}

      {treeListState.status === 'loaded' && visibleTrees.length > 0 && (
        <ul className="tree-list">
          {visibleTrees.map((tree) => (
            <li key={tree.id}>
              <Link to={`/projects/${projectId}/trees/${tree.id}`} className="tree-list-item">
                <span className="tree-list-item-number">Drzewo {tree.inventoryNumber}</span>
                {tree.speciesLatin && <span className="tree-list-item-species">{tree.speciesLatin}</span>}
                {!tree.speciesLatin && tree.speciesPolish && <span>{tree.speciesPolish}</span>}
                {tree.notes && <span className="tree-list-item-notes">{tree.notes}</span>}
                {/* Pokazujemy tylko brak - to drzewa, ktore wymagaja jeszcze ustalenia punktu. */}
                {!tree.location && <span className="tree-list-item-no-location">Brak lokalizacji</span>}
                {categoryByTreeId.get(tree.id) && <ValuationBadge category={categoryByTreeId.get(tree.id)} prefix="Waloryzacja: " />}
                {latestByTreeId.get(tree.id) && <HealthStatusBadge status={latestByTreeId.get(tree.id)!.healthStatus} prefix="Stan: " />}
                {(markingCodesByTreeId.get(tree.id) ?? []).length > 0 && (
                  <span className="tree-marking-list" aria-label="Oznaczenia">
                    {markingCodesByTreeId.get(tree.id)!.map((code) => markingByCode.get(code)).filter((marking) => marking !== undefined)
                      .map((marking) => <TreeMarkingBadge key={marking.code} marking={marking} />)}
                  </span>
                )}
                {pendingTreeIds.has(tree.id) && <span className="badge offline-pending-badge">Czeka na synchronizację</span>}
              </Link>
            </li>
          ))}
        </ul>
      )}

      {isNewTreeDialogOpen && (
        <NewTreeDialog projectId={projectId} closeDialog={() => setIsNewTreeDialogOpen(false)} />
      )}
    </div>
  );
}

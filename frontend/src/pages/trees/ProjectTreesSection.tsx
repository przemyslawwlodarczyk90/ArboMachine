import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { useOfflineState } from '../../offline/useOfflineState';
import { parsePositiveIdRouteParam } from '../../routing/parsePositiveIdRouteParam';
import { NewTreeDialog } from './NewTreeDialog';
import type { Tree } from './treeDataTypes';
import { treesApi } from './treesApi';
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

  useEffect(() => {
    if (projectId === null) return;
    let isResponseStillWanted = true;
    setTreeListState({ status: 'loading' });
    treeValuationsApi.listProjectValuations(projectId)
      .then((loadedValuations) => { if (isResponseStillWanted) setValuations(loadedValuations); })
      .catch(() => { if (isResponseStillWanted) setValuations(null); });
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

  if (projectId === null) return null; // ProjectPage pokazuje juz stan "nie istnieje"
  const visibleTrees = treeListState.status === 'loaded'
    ? filterTreesByValuation(treeListState.trees, categoryByTreeId, valuationFilter) : [];

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

      {treeListState.status === 'loaded' && treeListState.trees.length > 0 && valuations !== null && (
        <div className="tree-list-filters">
          <div className="field">
            <label htmlFor="tree-valuation-filter">Waloryzacja</label>
            <select id="tree-valuation-filter" value={valuationFilter}
                    onChange={(changeEvent) => setValuationFilter(changeEvent.target.value as ValuationFilter)}>
              <option value="ALL">Wszystkie</option>
              <option value="NOT_ASSESSED">Nieocenione</option>
              {VALUATION_CATEGORY_OPTIONS.map(({ category, label }) => <option key={category} value={category}>{label}</option>)}
            </select>
          </div>
          <span className="muted small">Pokazano {visibleTrees.length} z {treeListState.trees.length}</span>
        </div>
      )}

      {treeListState.status === 'loaded' && treeListState.trees.length > 0 && visibleTrees.length === 0 && (
        <p className="muted">Żadne drzewo nie pasuje do wybranego filtra.</p>
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

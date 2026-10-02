import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { useOfflineState } from '../../../offline/useOfflineState';
import { parsePositiveIdRouteParam } from '../../../routing/parsePositiveIdRouteParam';
import type { Tree } from '../../trees/treeDataTypes';
import { treesApi } from '../../trees/treesApi';
import type { TreeWork } from '../../trees/works/treeWorkDataTypes';
import { toCompletedTreeWorkRequest } from '../../trees/works/treeWorkFormValues';
import { TreeWorkMeta } from '../../trees/works/TreeWorkMeta';
import { treeWorksApi } from '../../trees/works/treeWorksApi';
import {
  countWorks, DEFAULT_WORK_FILTER, filterWorks, formatWorkDate, isOpenWorkStatus, isWorkPastPlannedDate, sortWorksForManagement,
  todayIsoDate, TREE_WORK_PRIORITIES, TREE_WORK_STATUSES, workPriorityLabel, workStatusLabel, type WorkFilter,
} from '../../trees/works/workManagement';

type ManagementDataState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; trees: Tree[]; works: TreeWork[] };

/**
 * Sekcja "Gospodarka drzewostanem" workspace projektu (/projects/:projectId/management, etap 28): wszystkie prace
 * projektu (GET .../works) z drzewami (GET .../trees) laczone w pamieci po treeId. Filtry status / priorytet / drzewo
 * dzialaja lokalnie (takze offline, na kopii odczytow). Domyslnie "Do wykonania". Szeroki ekran - tabela,
 * waski - karty. Edycja pracy jest w karcie drzewa; tu tylko szybkie "Oznacz jako wykonaną".
 * Bez harmonogramu, kalendarza i zespolow (V1).
 */
export function ProjectManagementPage() {
  const projectId = parsePositiveIdRouteParam(useParams().projectId);
  const [dataState, setDataState] = useState<ManagementDataState>({ status: 'loading' });
  const [workFilter, setWorkFilter] = useState<WorkFilter>(DEFAULT_WORK_FILTER);
  const [completingWorkId, setCompletingWorkId] = useState<number | null>(null);
  const [actionErrorMessage, setActionErrorMessage] = useState<string | null>(null);
  const { syncGeneration } = useOfflineState();

  useEffect(() => {
    if (projectId === null) return;
    let isResponseStillWanted = true;
    setDataState({ status: 'loading' });
    Promise.all([treesApi.listTreesOfProject(projectId), treeWorksApi.listProjectWorks(projectId)])
      .then(([trees, works]) => { if (isResponseStillWanted) setDataState({ status: 'loaded', trees, works }); })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setDataState({
          status: 'failed',
          errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.',
        });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId, syncGeneration]);

  const loadedData = dataState.status === 'loaded' ? dataState : null;
  const treeById = useMemo(() => new Map((loadedData?.trees ?? []).map((tree) => [tree.id, tree])), [loadedData]);
  const visibleWorks = useMemo(
    () => (loadedData ? sortWorksForManagement(filterWorks(loadedData.works, workFilter)) : []), [loadedData, workFilter]);
  const counts = useMemo(() => countWorks(loadedData?.works ?? []), [loadedData]);
  // Drzewa do filtra: tylko te, ktore maja prace (krotsza lista), w kolejnosci numerow.
  const treesWithWorks = useMemo(() => {
    const treeIds = new Set((loadedData?.works ?? []).map((work) => work.treeId));
    return (loadedData?.trees ?? []).filter((tree) => treeIds.has(tree.id))
      .sort((first, second) => first.inventoryNumber.localeCompare(second.inventoryNumber, 'pl', { numeric: true }));
  }, [loadedData]);

  if (projectId === null) return null; // ProjectPage pokazuje juz stan "nie istnieje"

  const markAsCompleted = async (work: TreeWork) => {
    if (completingWorkId !== null) return;
    setCompletingWorkId(work.id);
    setActionErrorMessage(null);
    try {
      const updatedWork = await treeWorksApi.updateTreeWork(projectId, work.treeId, work.id, toCompletedTreeWorkRequest(work, todayIsoDate()));
      setDataState((currentState) => currentState.status === 'loaded'
        ? { ...currentState, works: currentState.works.map((item) => (item.id === updatedWork.id ? updatedWork : item)) }
        : currentState);
    } catch (savingError) {
      setActionErrorMessage(`Nie udało się oznaczyć pracy jako wykonanej. ${
        savingError instanceof ApiRequestError ? savingError.message : 'Nieoczekiwany błąd.'}`);
    } finally {
      setCompletingWorkId(null);
    }
  };

  if (dataState.status === 'loading') return <div className="center"><LoadingSpinner /></div>;

  if (dataState.status === 'failed') {
    return (
      <div className="stack">
        <h2>Gospodarka drzewostanem</h2>
        <Alert variant="error">Nie udało się załadować prac. {dataState.errorMessage}</Alert>
      </div>
    );
  }

  const today = todayIsoDate();
  const treeLabelOf = (treeId: number) => treeById.get(treeId)?.inventoryNumber ?? `#${treeId}`;
  const speciesOf = (treeId: number) => {
    const tree = treeById.get(treeId);
    return [tree?.speciesPolish, tree?.speciesLatin].filter(Boolean).join(' · ') || '—';
  };
  const treeLink = (treeId: number) => `/projects/${projectId}/trees/${treeId}`;
  const changeFilter = (partial: Partial<WorkFilter>) => setWorkFilter((current) => ({ ...current, ...partial }));
  const completeButton = (work: TreeWork) => isOpenWorkStatus(work.status) && (
    <button type="button" className="btn btn-secondary btn-small" disabled={completingWorkId !== null}
            onClick={() => markAsCompleted(work)}>
      {completingWorkId === work.id ? 'Zapisywanie…' : 'Oznacz jako wykonaną'}
    </button>
  );

  return (
    <div className="stack">
      <div className="section-header-row">
        <h2>Gospodarka drzewostanem</h2>
        <Link to="../map" relative="path" className="btn btn-secondary btn-small">Pokaż na mapie</Link>
      </div>
      <p className="muted small">
        Zalecenia i prace wszystkich drzew projektu. Statusy i priorytety to robocze oznaczenia wersji V1. Prace dodajesz
        i edytujesz w karcie drzewa.
      </p>

      {dataState.works.length === 0 ? (
        <Alert variant="info">
          W projekcie nie ma jeszcze zaleceń ani prac. Dodasz je w karcie drzewa (sekcja „Zalecenia i prace”).{' '}
          <Link to={`/projects/${projectId}/trees`}>Przejdź do listy drzew</Link>
        </Alert>
      ) : (
        <>
          <ul className="management-counts" aria-label="Liczba prac">
            <li>Wszystkie: <strong>{counts.ALL}</strong></li>
            <li>Do wykonania: <strong>{counts.OPEN}</strong></li>
            {TREE_WORK_STATUSES.map((status) => <li key={status}>{workStatusLabel(status)}: <strong>{counts[status]}</strong></li>)}
          </ul>

          <div className="management-filters">
            <div className="field">
              <label htmlFor="management-status-filter">Status</label>
              <select id="management-status-filter" value={workFilter.status}
                      onChange={(changeEvent) => changeFilter({ status: changeEvent.target.value as WorkFilter['status'] })}>
                <option value="OPEN">Do wykonania (zalecone i zaplanowane)</option>
                <option value="ALL">Wszystkie</option>
                {TREE_WORK_STATUSES.map((status) => <option key={status} value={status}>{workStatusLabel(status)}</option>)}
              </select>
            </div>
            <div className="field">
              <label htmlFor="management-priority-filter">Priorytet</label>
              <select id="management-priority-filter" value={workFilter.priority}
                      onChange={(changeEvent) => changeFilter({ priority: changeEvent.target.value as WorkFilter['priority'] })}>
                <option value="ALL">Wszystkie</option>
                {TREE_WORK_PRIORITIES.map((priority) => <option key={priority} value={priority}>{workPriorityLabel(priority)}</option>)}
                <option value="NONE">Nie określono</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="management-tree-filter">Drzewo</label>
              <select id="management-tree-filter" value={String(workFilter.treeId)}
                      onChange={(changeEvent) => changeFilter({
                        treeId: changeEvent.target.value === 'ALL' ? 'ALL' : Number(changeEvent.target.value),
                      })}>
                <option value="ALL">Wszystkie</option>
                {treesWithWorks.map((tree) => <option key={tree.id} value={tree.id}>{tree.inventoryNumber}</option>)}
              </select>
            </div>
            <span className="muted small">Pokazano {visibleWorks.length} z {counts.ALL}</span>
            {(workFilter.status !== DEFAULT_WORK_FILTER.status || workFilter.priority !== 'ALL' || workFilter.treeId !== 'ALL') && (
              <button type="button" className="btn btn-ghost btn-small" onClick={() => setWorkFilter(DEFAULT_WORK_FILTER)}>
                Wyczyść filtry
              </button>
            )}
          </div>

          {actionErrorMessage && <Alert variant="error">{actionErrorMessage}</Alert>}

          {visibleWorks.length === 0 ? (
            <p className="muted">Żadna praca nie pasuje do wybranych filtrów.</p>
          ) : (
            <>
              <table className="management-table">
                <thead>
                  <tr>
                    <th>Drzewo</th>
                    <th>Gatunek</th>
                    <th>Praca</th>
                    <th>Status</th>
                    <th>Priorytet</th>
                    <th>Termin</th>
                    <th>Wykonano</th>
                    <th><span className="visually-hidden">Akcje</span></th>
                  </tr>
                </thead>
                <tbody>
                  {visibleWorks.map((work) => (
                    <tr key={work.id}>
                      <td><Link to={treeLink(work.treeId)}>{treeLabelOf(work.treeId)}</Link></td>
                      <td>{speciesOf(work.treeId)}</td>
                      <td>
                        <strong>{work.name}</strong>
                        {work.offlinePending && <span className="badge offline-pending-badge">Czeka na synchronizację</span>}
                        {work.description && <div className="muted small">{work.description}</div>}
                      </td>
                      <td>
                        <span className={`badge work-status-badge work-status-${work.status ?? 'RECOMMENDED'}`}>{workStatusLabel(work.status)}</span>
                      </td>
                      <td className={work.priority ? `work-priority-${work.priority}` : 'muted'}>
                        {work.priority ? workPriorityLabel(work.priority) : '—'}
                      </td>
                      <td className={isWorkPastPlannedDate(work, today) ? 'work-overdue' : undefined}>
                        {formatWorkDate(work.plannedDate)}{isWorkPastPlannedDate(work, today) && ' (po terminie)'}
                      </td>
                      <td>{formatWorkDate(work.completedDate)}</td>
                      <td>{completeButton(work)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <ul className="management-cards" aria-label="Prace">
                {visibleWorks.map((work) => (
                  <li key={work.id} className="management-card">
                    <div>
                      <Link to={treeLink(work.treeId)}><strong>Drzewo {treeLabelOf(work.treeId)}</strong></Link>
                      <span className="muted small"> · {speciesOf(work.treeId)}</span>
                    </div>
                    <div>
                      <strong>{work.name}</strong>
                      {work.offlinePending && <span className="badge offline-pending-badge">Czeka na synchronizację</span>}
                    </div>
                    <TreeWorkMeta treeWork={work} />
                    {work.description && <p className="muted small tree-work-item-description">{work.description}</p>}
                    <div className="form-actions">{completeButton(work)}</div>
                  </li>
                ))}
              </ul>
            </>
          )}
        </>
      )}
    </div>
  );
}

import { useEffect, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { MapSymbolSwatch } from '../../../map/MapSymbolSwatch';
import { useMapSymbolCatalog } from '../../../map/useMapSymbolCatalog';
import { DeleteTreeWorkDialog } from './DeleteTreeWorkDialog';
import type { TreeWork } from './treeWorkDataTypes';
import { TreeWorkForm } from './TreeWorkForm';
import { emptyTreeWorkFormValues, toCompletedTreeWorkRequest, toTreeWorkFormValues } from './treeWorkFormValues';
import { TreeWorkMeta } from './TreeWorkMeta';
import { isOpenWorkStatus, todayIsoDate } from './workManagement';
import { TreeWorkMapSymbolEditor } from './TreeWorkMapSymbolEditor';
import { treeWorksApi } from './treeWorksApi';

type TreeWorkListState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; treeWorks: TreeWork[] };

/**
 * Sekcja "Zalecenia i prace" karty drzewa: lista prac (nazwa + opis), dodawanie, edycja w miejscu i usuwanie.
 * Ma wlasny stan listy; po kazdej operacji uzywa odpowiedzi backendu, bez przeladowania karty drzewa.
 * Celowo bez powiazania z mapa (symbole/kolory prac) - slownik prac nie jest jeszcze ustalony.
 */
export function TreeWorksSection({ projectId, treeId, syncGeneration = 0 }: {
  projectId: number;
  treeId: number;
  /** etap 26: po synchronizacji zmian offline lista jest pobierana ponownie */
  syncGeneration?: number;
}) {
  const [treeWorkListState, setTreeWorkListState] = useState<TreeWorkListState>({ status: 'loading' });
  const [isAddingWork, setIsAddingWork] = useState(false);
  const [editedTreeWorkId, setEditedTreeWorkId] = useState<number | null>(null);
  const [symbolEditedTreeWorkId, setSymbolEditedTreeWorkId] = useState<number | null>(null);
  const { catalogState, symbolByCode } = useMapSymbolCatalog();
  const [workAwaitingDeletion, setWorkAwaitingDeletion] = useState<TreeWork | null>(null);
  const [completingTreeWorkId, setCompletingTreeWorkId] = useState<number | null>(null);
  const [quickActionErrorMessage, setQuickActionErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isResponseStillWanted = true;
    setTreeWorkListState({ status: 'loading' });
    treeWorksApi.listTreeWorks(projectId, treeId)
      .then((treeWorks) => {
        if (isResponseStillWanted) setTreeWorkListState({ status: 'loaded', treeWorks });
      })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setTreeWorkListState({
          status: 'failed',
          errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.',
        });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId, treeId, syncGeneration]);

  const updateLoadedWorks = (changeTreeWorks: (treeWorks: TreeWork[]) => TreeWork[]) =>
    setTreeWorkListState((currentState) => currentState.status === 'loaded'
      ? { status: 'loaded', treeWorks: changeTreeWorks(currentState.treeWorks) }
      : currentState);

  const appendCreatedWork = (createdTreeWork: TreeWork) => {
    updateLoadedWorks((treeWorks) => [...treeWorks, createdTreeWork]);
    setIsAddingWork(false);
  };

  const replaceUpdatedWork = (updatedTreeWork: TreeWork) => {
    updateLoadedWorks((treeWorks) => treeWorks.map((treeWork) =>
      treeWork.id === updatedTreeWork.id ? updatedTreeWork : treeWork));
    setEditedTreeWorkId(null);
    setSymbolEditedTreeWorkId(null);
  };

  const removeDeletedWork = (deletedTreeWorkId: number) => {
    updateLoadedWorks((treeWorks) => treeWorks.filter((treeWork) => treeWork.id !== deletedTreeWorkId));
    setWorkAwaitingDeletion(null);
  };

  // Etap 28: szybkie "Oznacz jako wykonaną" - PATCH z kompletem pol (dziala tez offline, jak edycja).
  const markWorkAsCompleted = async (treeWork: TreeWork) => {
    if (completingTreeWorkId !== null) return;
    setCompletingTreeWorkId(treeWork.id);
    setQuickActionErrorMessage(null);
    try {
      replaceUpdatedWork(await treeWorksApi.updateTreeWork(projectId, treeId, treeWork.id,
        toCompletedTreeWorkRequest(treeWork, todayIsoDate())));
    } catch (savingError) {
      setQuickActionErrorMessage(`Nie udało się oznaczyć pracy jako wykonanej. ${
        savingError instanceof ApiRequestError ? savingError.message : 'Nieoczekiwany błąd.'}`);
    } finally {
      setCompletingTreeWorkId(null);
    }
  };

  const startAddingWork = () => {
    setEditedTreeWorkId(null);
    setIsAddingWork(true);
  };

  // Jeden aktywny formularz naraz: dodawanie, edycja jednej pracy albo zmiana jej symbolu.
  const isAnyFormOpen = isAddingWork || editedTreeWorkId !== null || symbolEditedTreeWorkId !== null;

  return (
    <section className="tree-card-section" aria-labelledby="tree-works-heading">
      <div className="section-header-row">
        <h3 id="tree-works-heading">Zalecenia i prace</h3>
        {treeWorkListState.status === 'loaded' && !isAnyFormOpen && (
          <button className="btn btn-secondary" onClick={startAddingWork}>Dodaj pracę</button>
        )}
      </div>

      {treeWorkListState.status === 'loading' && <div className="center"><LoadingSpinner /></div>}

      {treeWorkListState.status === 'failed' && (
        <Alert variant="error">Nie udało się wczytać zaleceń i prac. {treeWorkListState.errorMessage}</Alert>
      )}

      {quickActionErrorMessage && <Alert variant="error">{quickActionErrorMessage}</Alert>}

      {isAddingWork && (
        <TreeWorkForm initialFormValues={emptyTreeWorkFormValues} submitButtonLabel="Dodaj pracę"
                      saveTreeWork={(request) => treeWorksApi.createTreeWork(projectId, treeId, request)}
                      onTreeWorkSaved={appendCreatedWork} cancelEditing={() => setIsAddingWork(false)} />
      )}

      {treeWorkListState.status === 'loaded' && treeWorkListState.treeWorks.length === 0 && !isAddingWork && (
        <p className="muted">Nie dodano jeszcze żadnych zaleceń ani prac.</p>
      )}

      {treeWorkListState.status === 'loaded' && treeWorkListState.treeWorks.length > 0 && (
        <ul className="tree-work-list">
          {treeWorkListState.treeWorks.map((treeWork) => (
            <li key={treeWork.id} className="tree-work-item">
              {editedTreeWorkId === treeWork.id ? (
                <TreeWorkForm initialFormValues={toTreeWorkFormValues(treeWork)} submitButtonLabel="Zapisz"
                              saveTreeWork={(request) => treeWorksApi.updateTreeWork(projectId, treeId, treeWork.id, request)}
                              onTreeWorkSaved={replaceUpdatedWork} cancelEditing={() => setEditedTreeWorkId(null)} />
              ) : (
                <>
                  <div className="tree-work-item-text">
                    <strong>{treeWork.name}</strong>
                    {treeWork.offlinePending && <span className="badge offline-pending-badge">Czeka na synchronizację</span>}
                    <TreeWorkMeta treeWork={treeWork} />
                    {treeWork.description && <p className="tree-work-item-description">{treeWork.description}</p>}
                    <span className="muted small map-symbol-inline">
                      Symbol na mapie: {treeWork.mapSymbolCode && symbolByCode.get(treeWork.mapSymbolCode)
                        ? <><MapSymbolSwatch symbol={symbolByCode.get(treeWork.mapSymbolCode)!} size={16} /> {symbolByCode.get(treeWork.mapSymbolCode)!.displayName}</>
                        : 'brak'}
                    </span>
                  </div>
                  {symbolEditedTreeWorkId === treeWork.id && catalogState.status === 'loaded' && (
                    <TreeWorkMapSymbolEditor projectId={projectId} treeId={treeId} treeWork={treeWork}
                                             symbols={catalogState.symbols} onTreeWorkChanged={replaceUpdatedWork}
                                             closeEditor={() => setSymbolEditedTreeWorkId(null)} />
                  )}
                  {!isAnyFormOpen && (
                    <div className="form-actions">
                      {isOpenWorkStatus(treeWork.status) && (
                        <button className="btn btn-primary btn-small" disabled={completingTreeWorkId !== null}
                                onClick={() => markWorkAsCompleted(treeWork)}>
                          {completingTreeWorkId === treeWork.id ? 'Zapisywanie…' : 'Oznacz jako wykonaną'}
                        </button>
                      )}
                      <button className="btn btn-secondary btn-small" onClick={() => setEditedTreeWorkId(treeWork.id)}>
                        Edytuj
                      </button>
                      {catalogState.status === 'loaded' && treeWork.id > 0 && (
                        <button className="btn btn-secondary btn-small" onClick={() => setSymbolEditedTreeWorkId(treeWork.id)}>
                          Zmień symbol
                        </button>
                      )}
                      {treeWork.id > 0 && (
                        <button className="btn btn-danger-outline btn-small" onClick={() => setWorkAwaitingDeletion(treeWork)}>
                          Usuń
                        </button>
                      )}
                    </div>
                  )}
                </>
              )}
            </li>
          ))}
        </ul>
      )}

      {workAwaitingDeletion && (
        <DeleteTreeWorkDialog projectId={projectId} treeId={treeId} treeWork={workAwaitingDeletion}
                              onTreeWorkDeleted={removeDeletedWork} closeDialog={() => setWorkAwaitingDeletion(null)} />
      )}
    </section>
  );
}

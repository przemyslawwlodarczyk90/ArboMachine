import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { formatTimestampForDisplay } from '../../formatting/formatTimestampForDisplay';
import { offlineService } from '../../offline/offlineService';
import { useOfflineState } from '../../offline/useOfflineState';
import { parsePositiveIdRouteParam } from '../../routing/parsePositiveIdRouteParam';
import { parseTreeIdRouteParam } from '../../routing/parseTreeIdRouteParam';
import { TreeLocationSection } from './location/TreeLocationSection';
import { NewTreeDialog } from './NewTreeDialog';
import { TreePhotosSection } from './photos/TreePhotosSection';
import type { Tree } from './treeDataTypes';
import { TreeDeletionSection } from './deletion/TreeDeletionSection';
import { TreeFieldForm } from './TreeFieldForm';
import { TreeMapSymbolSection } from './TreeMapSymbolSection';
import { TreeMarkingsSection } from './markings/TreeMarkingsSection';
import { treesApi } from './treesApi';
import { TreeMonitoringSection } from './monitoring/TreeMonitoringSection';
import { TreeValuationSection } from './valuation/TreeValuationSection';
import { TreeWorksSection } from './works/TreeWorksSection';

type TreeLoadingState =
  | { status: 'loading' }
  | { status: 'loaded'; tree: Tree }
  /** 404 z backendu (drzewo nie istnieje, jest w innym projekcie albo projekt nie nalezy do uzytkownika). */
  | { status: 'notFoundOrNoAccess' }
  | { status: 'failed'; errorMessage: string };

/**
 * Karta drzewa (/projects/:projectId/trees/:treeId) - ekran pracy przy jednym drzewie. Strona tylko laduje drzewo
 * i sklada sekcje: Dane drzewa (TreeFieldForm - formularz terenowy), Lokalizacja, Waloryzacja, Monitoring stanu,
 * Symbol drzewa na mapie, Zdjecia, Zalecenia i prace, Usun drzewo (etap 44).
 * Kazda sekcja ma wlasny stan i wlasne operacje zapisu; drzewo z odpowiedzi backendu jest jednym zrodlem prawdy
 * dla formularza i lokalizacji. Wlasnosci nie sprawdzamy w React.
 */
export function TreePage() {
  const routeParams = useParams();
  const projectId = parsePositiveIdRouteParam(routeParams.projectId);
  // Ujemne id = drzewo utworzone bez polaczenia (etap 26), dostepne z kopii na urzadzeniu do synchronizacji.
  const treeId = parseTreeIdRouteParam(routeParams.treeId);
  const navigate = useNavigate();
  const { syncGeneration, pendingTreeIds } = useOfflineState();
  const [treeLoadingState, setTreeLoadingState] = useState<TreeLoadingState>({ status: 'loading' });
  const [hasUnsavedTreeChanges, setHasUnsavedTreeChanges] = useState(false);
  const [isNewTreeDialogOpen, setIsNewTreeDialogOpen] = useState(false);

  useEffect(() => {
    if (projectId === null || treeId === null) {
      setTreeLoadingState({ status: 'notFoundOrNoAccess' });
      return;
    }
    let isResponseStillWanted = true;
    setTreeLoadingState((currentState) => (currentState.status === 'loaded' && currentState.tree.id === treeId
      ? currentState : { status: 'loading' }));
    setHasUnsavedTreeChanges(false);
    // Drzewo utworzone offline, ktore juz trafilo na serwer -> karta pod nowym (prawdziwym) adresem.
    if (treeId < 0) {
      void offlineService.serverTreeIdOf(treeId).then((serverTreeId) => {
        if (isResponseStillWanted && serverTreeId) navigate(`/projects/${projectId}/trees/${serverTreeId}`, { replace: true });
      });
    }
    treesApi.getTreeOfProject(projectId, treeId)
      .then((tree) => {
        if (isResponseStillWanted) setTreeLoadingState({ status: 'loaded', tree });
      })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        const isNotFoundOrForbidden = loadingError instanceof ApiRequestError
          && (loadingError.httpStatus === 404 || loadingError.httpStatus === 403);
        setTreeLoadingState(isNotFoundOrForbidden
          ? { status: 'notFoundOrNoAccess' }
          : { status: 'failed', errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId, treeId, syncGeneration, navigate]);

  // Stabilna funkcja - formularz zglasza przez nia zmiane stanu "niezapisane zmiany" w efekcie.
  const replaceTree = useCallback((changedTree: Tree) => setTreeLoadingState({ status: 'loaded', tree: changedTree }), []);

  const backToTreesLink = <Link to={`/projects/${projectId ?? ''}/trees`} className="back-link">← Wróć do listy drzew</Link>;

  if (treeLoadingState.status === 'loading') {
    return <div className="center"><LoadingSpinner /></div>;
  }

  if (treeLoadingState.status === 'notFoundOrNoAccess') {
    return (
      <div className="stack">
        {backToTreesLink}
        <div className="empty-state">
          <p>To drzewo nie istnieje albo nie masz do niego dostępu.</p>
        </div>
      </div>
    );
  }

  if (treeLoadingState.status === 'failed') {
    return (
      <div className="stack">
        {backToTreesLink}
        <Alert variant="error">Nie udało się pobrać drzewa. {treeLoadingState.errorMessage}</Alert>
      </div>
    );
  }

  const { tree } = treeLoadingState;
  if (projectId === null) return null;
  const isCreatedOffline = tree.id < 0;
  const hasPendingChanges = pendingTreeIds.has(tree.id);

  return (
    <div className="stack">
      {backToTreesLink}
      <div className="section-header-row">
        <div className="stack-tight">
          <h2>Drzewo {tree.inventoryNumber}</h2>
          <span className="muted small">
            Dodano {formatTimestampForDisplay(tree.createdAt)} · Ostatnia zmiana {formatTimestampForDisplay(tree.updatedAt)}
          </span>
        </div>
        {/* V1 "nastepnego drzewa": szybkie dodanie kolejnego drzewa bez wracania do listy. Z niezapisanymi zmianami
            przycisk jest zablokowany, zeby nie zgubic wpisanych danych. */}
        <button className="btn btn-secondary" onClick={() => setIsNewTreeDialogOpen(true)} disabled={hasUnsavedTreeChanges}
                title={hasUnsavedTreeChanges ? 'Najpierw zapisz albo anuluj zmiany w danych drzewa.' : undefined}>
          Dodaj następne drzewo
        </button>
      </div>

      {(isCreatedOffline || hasPendingChanges) && (
        <Alert variant="info">
          {isCreatedOffline
            ? 'To drzewo zostało dodane bez połączenia i jest zapisane na tym urządzeniu. Zostanie wysłane po połączeniu z internetem.'
            : 'Zmiany tego drzewa są zapisane na tym urządzeniu i czekają na wysłanie.'}
        </Alert>
      )}

      {/* key: przejscie na inne drzewo zaczyna formularz od zera (bez resztek poprzedniego drzewa). */}
      <TreeFieldForm key={tree.id} projectId={projectId} savedTree={tree} onTreeSaved={replaceTree}
                     onUnsavedChangesChange={setHasUnsavedTreeChanges} />
      <TreeLocationSection projectId={projectId} tree={tree} onTreeChanged={replaceTree} />
      <TreeMarkingsSection key={`markings-${tree.id}`} projectId={projectId} treeId={tree.id} syncGeneration={syncGeneration} />
      <TreeValuationSection key={`valuation-${tree.id}`} projectId={projectId} treeId={tree.id} syncGeneration={syncGeneration} />
      <TreeMonitoringSection key={`monitoring-${tree.id}`} projectId={projectId} treeId={tree.id} syncGeneration={syncGeneration} />
      {isCreatedOffline ? (
        <p className="muted tree-offline-note">Symbol na mapie można dodać po synchronizacji drzewa z serwerem.</p>
      ) : (
        <TreeMapSymbolSection key={`symbol-${tree.id}`} projectId={projectId} tree={tree} onTreeChanged={replaceTree} />
      )}
      {/* Etap 33: zdjecia takze dla drzewa utworzonego offline - czekaja na urzadzeniu, az drzewo trafi na serwer. */}
      <TreePhotosSection key={`photos-${tree.id}`} projectId={projectId} treeId={tree.id} />
      <TreeWorksSection key={`works-${tree.id}`} projectId={projectId} treeId={tree.id} syncGeneration={syncGeneration} />
      <TreeDeletionSection key={`deletion-${tree.id}`} projectId={projectId} treeId={tree.id} inventoryNumber={tree.inventoryNumber}
                           isCreatedOffline={isCreatedOffline} hasPendingChanges={hasPendingChanges} />

      {isNewTreeDialogOpen && (
        <NewTreeDialog projectId={projectId} closeDialog={() => setIsNewTreeDialogOpen(false)} />
      )}
    </div>
  );
}

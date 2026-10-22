import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiRequestError } from '../api/httpClient';
import { Alert } from '../components/Alert';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { offlineService } from '../offline/offlineService';
import { useOfflineState } from '../offline/useOfflineState';
import type { TreeLocation, Tree } from '../pages/trees/treeDataTypes';
import { TreeFieldForm } from '../pages/trees/TreeFieldForm';
import type { MeasurementMode } from '../pages/trees/location/TreeGnssLocationPanel';
import { TreeMarkingBadge } from '../pages/trees/markings/TreeMarkingBadge';
import type { TreeMarkingDefinition } from '../pages/trees/markings/treeMarkingDataTypes';
import { TreeMarkingsSection } from '../pages/trees/markings/TreeMarkingsSection';
import { TreePhotosSection } from '../pages/trees/photos/TreePhotosSection';
import { treesApi } from '../pages/trees/treesApi';
import type { TreeWork } from '../pages/trees/works/treeWorkDataTypes';
import { TreeWorksSection } from '../pages/trees/works/TreeWorksSection';
import { rememberWorkMode } from './fieldModePreference';
import { FieldTreeLocationPanel } from './FieldTreeLocationPanel';
import { useUnsavedChangesGuard } from './UnsavedChangesDialog';

/** Wysokosc panelu od dolu na waskim ekranie (od 900 px panel jest z boku na cala wysokosc). */
export type FieldSheetSize = 'COLLAPSED' | 'HALF' | 'FULL';

const LARGER_SIZE: Record<FieldSheetSize, FieldSheetSize> = { COLLAPSED: 'HALF', HALF: 'FULL', FULL: 'FULL' };
const SMALLER_SIZE: Record<FieldSheetSize, FieldSheetSize> = { COLLAPSED: 'COLLAPSED', HALF: 'COLLAPSED', FULL: 'HALF' };

type SheetTreeState =
  | { status: 'loading' }
  | { status: 'loaded'; tree: Tree }
  | { status: 'notFoundOrNoAccess' }
  | { status: 'failed'; errorMessage: string };

/**
 * Panel drzewa w trybie terenowym (etap 32) - te same sekcje co karta biurowa (bez kopii komponentow), w kolejnosci
 * pracy w terenie (V1): dane drzewa, oznaczenia, zdjecia, zalecenia i prace, lokalizacja (bez drugiej mapy),
 * link do pelnej karty (waloryzacja i kontrole stanu tylko w biurze). Kazda zmiana od razu zmienia wyglad drzewa na
 * mapie (callbacki do useProjectMapData). Drzewo utworzone offline (ujemne id) dziala jak w TreePage: komunikat
 * "zapisane na urzadzeniu" i przejscie na prawdziwe id po synchronizacji. Zdjecia offline: osobny, nastepny etap.
 */
export function FieldTreeSheet({ projectId, treeId, listedTree, markingCodes, markingByCode, size, onSizeChange,
                                 onTreeChanged, onTreeIdReplaced, onMarkingsSaved, onWorksChanged, onPendingLocationChanged,
                                 pickLocationOnMap, stopPickingOnMap, pickedLocation, measurementMode, onMeasurementModeChange,
                                 onMeasuringChange, closeSheet, startNextTree }: {
  projectId: number;
  treeId: number;
  /** drzewo z listy projektu - pokazywane od razu, zanim przyjdzie karta drzewa */
  listedTree: Tree | null;
  markingCodes: string[];
  markingByCode: Map<string, TreeMarkingDefinition>;
  size: FieldSheetSize;
  onSizeChange: (size: FieldSheetSize) => void;
  onTreeChanged: (tree: Tree) => void;
  onTreeIdReplaced: (localTreeId: number, serverTreeId: number) => void;
  onMarkingsSaved: (treeId: number, codes: string[]) => void;
  onWorksChanged: (treeId: number, works: TreeWork[]) => void;
  onPendingLocationChanged: (location: TreeLocation | null) => void;
  pickLocationOnMap: () => void;
  stopPickingOnMap: () => void;
  pickedLocation: TreeLocation | null;
  measurementMode: MeasurementMode;
  onMeasurementModeChange: (mode: MeasurementMode) => void;
  onMeasuringChange: (isMeasuring: boolean) => void;
  /** zamkniecie i "Nastepne drzewo" - rodzic sam pyta o niezapisane zmiany */
  closeSheet: () => void;
  startNextTree: () => void;
}) {
  const navigate = useNavigate();
  const { syncGeneration, pendingTreeIds, isOnline } = useOfflineState();
  const [treeState, setTreeState] = useState<SheetTreeState>(listedTree ? { status: 'loaded', tree: listedTree } : { status: 'loading' });
  const { runGuarded, dialog } = useUnsavedChangesGuard();

  useEffect(() => {
    let isResponseStillWanted = true;
    setTreeState((currentState) => (currentState.status === 'loaded' && currentState.tree.id === treeId
      ? currentState : listedTree?.id === treeId ? { status: 'loaded', tree: listedTree } : { status: 'loading' }));
    // Drzewo utworzone offline, ktore juz trafilo na serwer -> panel pod prawdziwym id.
    if (treeId < 0) {
      void offlineService.serverTreeIdOf(treeId).then((serverTreeId) => {
        if (isResponseStillWanted && serverTreeId) onTreeIdReplaced(treeId, serverTreeId);
      });
    }
    treesApi.getTreeOfProject(projectId, treeId)
      .then((tree) => { if (isResponseStillWanted) setTreeState({ status: 'loaded', tree }); })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        const isNotFound = loadingError instanceof ApiRequestError && (loadingError.httpStatus === 404 || loadingError.httpStatus === 403);
        setTreeState((currentState) => (currentState.status === 'loaded' ? currentState : isNotFound
          ? { status: 'notFoundOrNoAccess' }
          : { status: 'failed', errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' }));
      });
    return () => { isResponseStillWanted = false; };
    // listedTree celowo poza zaleznosciami - tylko punkt startowy; zrodlem prawdy jest karta drzewa z API.
  }, [projectId, treeId, syncGeneration, onTreeIdReplaced]);

  // Zapis formularza albo lokalizacji: panel i mapa dostaja to samo drzewo z odpowiedzi (albo z kolejki offline).
  const replaceTree = useCallback((changedTree: Tree) => {
    setTreeState({ status: 'loaded', tree: changedTree });
    onTreeChanged(changedTree);
  }, [onTreeChanged]);

  const openFullOfficeCard = () => runGuarded(() => {
    rememberWorkMode('OFFICE');
    navigate(`/projects/${projectId}/trees/${treeId}`);
  });

  const tree = treeState.status === 'loaded' ? treeState.tree : null;
  const isCreatedOffline = treeId < 0;
  const hasPendingChanges = pendingTreeIds.has(treeId);
  const markings = markingCodes.map((code) => markingByCode.get(code)).filter((marking) => marking !== undefined);

  return (
    <aside className={`field-sheet field-sheet-${size}`} aria-label={tree ? `Drzewo ${tree.inventoryNumber}` : 'Drzewo'}>
      <div className="field-sheet-header">
        <div className="field-sheet-heading">
          <h2>{tree ? `Drzewo ${tree.inventoryNumber}` : 'Drzewo'}</h2>
          {tree && (tree.speciesPolish || tree.speciesLatin) && (
            <span className="muted small">{[tree.speciesPolish, tree.speciesLatin].filter(Boolean).join(' · ')}</span>
          )}
          {markings.length > 0 && (
            <span className="tree-marking-list" aria-label="Oznaczenia drzewa">
              {markings.map((marking) => <TreeMarkingBadge key={marking.code} marking={marking} />)}
            </span>
          )}
          {(isCreatedOffline || hasPendingChanges) && (
            <span className="badge offline-pending-badge">{isCreatedOffline ? 'Zapisane na urządzeniu' : 'Czeka na synchronizację'}</span>
          )}
        </div>
        <button type="button" className="btn btn-secondary field-sheet-size-toggle"
                aria-label={size === 'FULL' ? 'Zmniejsz panel' : 'Powiększ panel'}
                onClick={() => onSizeChange(size === 'FULL' ? SMALLER_SIZE[size] : LARGER_SIZE[size])}>
          {size === 'FULL' ? '▾' : '▴'}
        </button>
        {size !== 'COLLAPSED' && (
          <button type="button" className="btn btn-secondary field-sheet-size-toggle" aria-label="Zwiń panel"
                  onClick={() => onSizeChange('COLLAPSED')}>—</button>
        )}
        <button type="button" className="btn btn-secondary" aria-label="Zamknij panel drzewa" onClick={closeSheet}>✕</button>
      </div>

      <div className="field-sheet-body">
        {treeState.status === 'loading' && <div className="center"><LoadingSpinner /></div>}
        {treeState.status === 'notFoundOrNoAccess' && <p>To drzewo nie istnieje albo nie masz do niego dostępu.</p>}
        {treeState.status === 'failed' && <Alert variant="error">Nie udało się pobrać drzewa. {treeState.errorMessage}</Alert>}
        {tree && (
          <>
            {(isCreatedOffline || hasPendingChanges) && (
              <Alert variant="info">
                {isCreatedOffline
                  ? 'To drzewo zostało dodane bez połączenia i jest zapisane na tym urządzeniu. Zostanie wysłane po połączeniu z internetem.'
                  : 'Zmiany tego drzewa są zapisane na tym urządzeniu i czekają na wysłanie.'}
              </Alert>
            )}
            {/* key: przejscie na inne drzewo zaczyna formularz od zera (bez resztek poprzedniego drzewa). */}
            <TreeFieldForm key={tree.id} projectId={projectId} savedTree={tree} onTreeSaved={replaceTree} />
            <TreeMarkingsSection key={`markings-${tree.id}`} projectId={projectId} treeId={tree.id} syncGeneration={syncGeneration}
                                 onMarkingsSaved={(codes) => onMarkingsSaved(tree.id, codes)} />
            {isCreatedOffline ? (
              <p className="muted tree-offline-note">Zdjęcia można dodać po synchronizacji drzewa z serwerem.</p>
            ) : (
              <>
                {!isOnline && (
                  <Alert variant="info">
                    Zdjęcia wymagają połączenia. Możesz zrobić je aparatem i dodać później, ale zapisz numer drzewa, bo
                    zdjęcie nie przypisze się samo.
                  </Alert>
                )}
                <TreePhotosSection key={`photos-${tree.id}`} projectId={projectId} treeId={tree.id} offersCameraCapture />
              </>
            )}
            <TreeWorksSection key={`works-${tree.id}`} projectId={projectId} treeId={tree.id} syncGeneration={syncGeneration}
                              onWorksChanged={(works) => onWorksChanged(tree.id, works)} />
            <FieldTreeLocationPanel key={`location-${tree.id}`} projectId={projectId} tree={tree} onTreeChanged={replaceTree}
                                    onPendingLocationChanged={onPendingLocationChanged} pickLocationOnMap={pickLocationOnMap}
                                    stopPickingOnMap={stopPickingOnMap} pickedLocation={pickedLocation}
                                    measurementMode={measurementMode} onMeasurementModeChange={onMeasurementModeChange}
                                    onMeasuringChange={onMeasuringChange} />
            <button type="button" className="btn btn-ghost" onClick={openFullOfficeCard}>
              Pełna karta (widok biurowy) - waloryzacja i kontrole stanu
            </button>
          </>
        )}
      </div>

      <div className="field-sheet-actions">
        <button type="button" className="btn btn-primary" onClick={startNextTree}>Następne drzewo</button>
        <button type="button" className="btn btn-secondary" onClick={closeSheet}>Zamknij</button>
      </div>
      {dialog}
    </aside>
  );
}

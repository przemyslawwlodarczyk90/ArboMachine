import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { ApiRequestError } from '../api/httpClient';
import { Alert } from '../components/Alert';
import { LoadingSpinner } from '../components/LoadingSpinner';
import type { GnssReading } from '../gnss/deviceGnss';
import { useLivePosition } from '../gnss/useLivePosition';
import { buildProjectMapLegend } from '../map/mapSymbolLegend';
import type { Project } from '../pages/projects/projectDataTypes';
import { projectsApi } from '../pages/projects/projectsApi';
import type { TreeStand } from '../pages/projects/stands/standDataTypes';
import { suggestNextInventoryNumber } from '../pages/trees/inventoryNumberSuggestion';
import { NewTreeDialog } from '../pages/trees/NewTreeDialog';
import { TreeGnssLocationPanel, type MeasurementMode } from '../pages/trees/location/TreeGnssLocationPanel';
import type { MapViewMode } from '../pages/trees/map/mapViewMode';
import { ProjectMapModeLegend } from '../pages/trees/map/ProjectMapModeLegend';
import { ProjectTreeMap, type MapCenterRequest } from '../pages/trees/map/ProjectTreeMap';
import { useProjectMapData } from '../pages/trees/map/useProjectMapData';
import type { Tree, TreeLocation } from '../pages/trees/treeDataTypes';
import { parsePositiveIdRouteParam } from '../routing/parsePositiveIdRouteParam';
import { FieldGpsIndicator } from './FieldGpsIndicator';
import { FieldLayersSheet } from './FieldLayersSheet';
import { useFieldTopBar } from './FieldLayout';
import {
  readFieldGnssAveraging, readFieldMapViewMode, saveFieldGnssAveraging, saveFieldMapViewMode,
} from './fieldMapPreferences';
import { SELECTED_TREE_SEARCH_PARAM, selectedTreeIdOf } from './fieldModePaths';
import { browserStorageOrNull } from './fieldModePreference';
import { FieldOfflinePreparation } from './FieldOfflinePreparation';
import { FieldTreeSheet, type FieldSheetSize } from './FieldTreeSheet';
import { useUnsavedChangesGuard } from './UnsavedChangesDialog';

type ProjectLoadingState =
  | { status: 'loading' }
  | { status: 'notFoundOrNoAccess' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; project: Project };

/**
 * Co robi dotkniecie mapy: zwykly podglad (dotkniecie drzewa otwiera panel), wskazanie miejsca nowego drzewa
 * albo wskazanie nowego punktu wybranego drzewa ("Przesun na mapie" w panelu).
 */
type MapInteraction = 'NONE' | 'MEASURING_NEW_TREE' | 'PICKING_NEW_TREE' | 'PICKING_TREE_LOCATION';

const NO_STANDS: TreeStand[] = [];

/**
 * Mapa projektu w trybie terenowym (/field/projects/:projectId?drzewo=ID, etap 32): mapa na caly ekran pod paskiem,
 * biezaca pozycja, przyciski plywajace ("+ Drzewo tutaj", "Wskaz na mapie", "Moja pozycja", "Warstwy") i panel
 * wybranego drzewa nad mapa. Te same dane i API co mapa biurowa (useProjectMapData) - praca offline jak dotad.
 * Mapa nie zmienia przyblizenia po zapisie drzewa (fitViewOnPointsChange="firstLoadOnly").
 */
export function FieldProjectPage() {
  const projectId = parsePositiveIdRouteParam(useParams().projectId);
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedTreeId = selectedTreeIdOf(searchParams.toString());
  const [projectLoadingState, setProjectLoadingState] = useState<ProjectLoadingState>({ status: 'loading' });
  const setTopBarContent = useFieldTopBar();
  const mapData = useProjectMapData(projectId);
  // Pomiar drzewa wstrzymuje sledzenie na zywo: przy aktywnym watchPosition Chrome potrafi nie oddac nowego
  // odczytu do getCurrentPosition(maximumAge: 0), gdy pozycja sie nie zmienia (sprawdzone na emulowanej pozycji).
  const [isMeasuringInSheet, setIsMeasuringInSheet] = useState(false);
  const [mapInteraction, setMapInteraction] = useState<MapInteraction>('NONE');
  const { liveStatus, lastReading } = useLivePosition(mapInteraction !== 'MEASURING_NEW_TREE' && !isMeasuringInSheet);
  const [viewMode, setViewMode] = useState<MapViewMode>(() => readFieldMapViewMode(browserStorageOrNull()));
  const [measurementMode, setMeasurementMode] = useState<MeasurementMode>(
    () => (readFieldGnssAveraging(browserStorageOrNull()) ? 'AVERAGED' : 'SINGLE'));
  const [pendingLocation, setPendingLocation] = useState<TreeLocation | null>(null);
  const [newTreeLocation, setNewTreeLocation] = useState<TreeLocation | null>(null);
  const [sheetSize, setSheetSize] = useState<FieldSheetSize>('HALF');
  const [isLayersSheetOpen, setIsLayersSheetOpen] = useState(false);
  const [areStandsVisible, setAreStandsVisible] = useState(true);
  const [centerRequest, setCenterRequest] = useState<MapCenterRequest | null>(null);
  const { runGuarded, dialog: unsavedChangesDialog } = useUnsavedChangesGuard();

  useEffect(() => {
    if (projectId === null) {
      setProjectLoadingState({ status: 'notFoundOrNoAccess' });
      return;
    }
    let isResponseStillWanted = true;
    projectsApi.getProjectOfCurrentUser(projectId)
      .then((project) => { if (isResponseStillWanted) setProjectLoadingState({ status: 'loaded', project }); })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        const isNotFound = loadingError instanceof ApiRequestError && (loadingError.httpStatus === 404 || loadingError.httpStatus === 403);
        setProjectLoadingState(isNotFound ? { status: 'notFoundOrNoAccess' }
          : { status: 'failed', errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId]);

  const projectName = projectLoadingState.status === 'loaded' ? projectLoadingState.project.name : null;
  useEffect(() => {
    setTopBarContent({ title: projectName, showsBackLink: true,
      gpsIndicator: <FieldGpsIndicator liveStatus={liveStatus} lastReading={lastReading} /> });
  }, [setTopBarContent, projectName, liveStatus, lastReading]);

  const { mapDataState, allTreeMapPoints, stands, referenceLayers, projectMarkings, upsertTree, replaceTreeId,
    replaceTreeMarkings, replaceTreeWorks, symbolCatalog: { symbolByCode }, markingCatalog } = mapData;
  const trees = useMemo(() => (mapDataState.status === 'loaded' ? mapDataState.trees : []), [mapDataState]);
  const legendEntries = useMemo(() => (mapDataState.status === 'loaded'
    ? buildProjectMapLegend(trees.map((tree) => ({ id: tree.id, hasLocation: !!tree.location, mapSymbolCode: tree.mapSymbolCode })),
      mapDataState.works, symbolByCode)
    : []), [mapDataState, trees, symbolByCode]);

  /** Wybrane drzewo w adresie: replace, zeby "wstecz" nie przechodzilo po wszystkich ogladanych drzewach. */
  const selectTree = useCallback((treeId: number | null) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current);
      if (treeId === null) next.delete(SELECTED_TREE_SEARCH_PARAM); else next.set(SELECTED_TREE_SEARCH_PARAM, String(treeId));
      return next;
    }, { replace: true });
  }, [setSearchParams]);

  const stopMapInteraction = useCallback(() => {
    setMapInteraction('NONE');
    setPendingLocation(null);
  }, []);

  const changeViewMode = (mode: MapViewMode) => {
    setViewMode(mode);
    saveFieldMapViewMode(browserStorageOrNull(), mode);
  };

  const changeMeasurementMode = useCallback((mode: MeasurementMode) => {
    setMeasurementMode(mode);
    saveFieldGnssAveraging(browserStorageOrNull(), mode === 'AVERAGED');
  }, []);

  // "+ Drzewo tutaj" i "Nastepne drzewo": najpierw decyzja o niezapisanych zmianach, potem pomiar GPS nowego punktu.
  const startTreeHere = () => runGuarded(() => {
    selectTree(null);
    setIsLayersSheetOpen(false);
    setPendingLocation(null);
    setMapInteraction('MEASURING_NEW_TREE');
  });

  const startPickingNewTree = () => runGuarded(() => {
    selectTree(null);
    setIsLayersSheetOpen(false);
    setPendingLocation(null);
    setMapInteraction((current) => (current === 'PICKING_NEW_TREE' ? 'NONE' : 'PICKING_NEW_TREE'));
  });

  const confirmMeasuredNewTree = (reading: GnssReading) => {
    setNewTreeLocation({ latitude: reading.latitude, longitude: reading.longitude, accuracyM: reading.accuracyM,
      altitudeM: reading.altitudeM, source: 'DEVICE_GNSS' });
    setMapInteraction('NONE');
  };

  const handleEmptyMapClick = (location: TreeLocation) => {
    if (mapInteraction === 'PICKING_NEW_TREE') {
      setPendingLocation(location);
      setNewTreeLocation({ ...location, source: 'MAP' });
      setMapInteraction('NONE');
    } else if (mapInteraction === 'PICKING_TREE_LOCATION') {
      setPendingLocation(location);
    }
  };

  const showCreatedTree = (createdTree: Tree) => {
    upsertTree(createdTree);
    setPendingLocation(null);
    setSheetSize('HALF');
    selectTree(createdTree.id);
  };

  const openTree = (treeId: number | null) => {
    if (treeId === null || treeId === selectedTreeId) return;
    runGuarded(() => {
      stopMapInteraction();
      setSheetSize((size) => (size === 'COLLAPSED' ? 'HALF' : size));
      selectTree(treeId);
    });
  };

  const closeSheet = () => runGuarded(() => {
    stopMapInteraction();
    selectTree(null);
  });

  const centerOnMyPosition = () => {
    if (!lastReading) return;
    setCenterRequest((current) => ({ latitude: lastReading.latitude, longitude: lastReading.longitude,
      requestId: (current?.requestId ?? 0) + 1 }));
  };

  const replaceSelectedTreeId = useCallback((localTreeId: number, serverTreeId: number) => {
    replaceTreeId(localTreeId, serverTreeId);
    selectTree(serverTreeId);
  }, [replaceTreeId, selectTree]);

  const onMarkingsSaved = useCallback((treeId: number, codes: string[]) => replaceTreeMarkings(treeId, codes), [replaceTreeMarkings]);
  const onWorksChanged = useCallback(replaceTreeWorks, [replaceTreeWorks]);

  if (projectLoadingState.status === 'notFoundOrNoAccess') {
    return <div className="field-padded"><p>Ten projekt nie istnieje albo nie masz do niego dostępu.</p></div>;
  }
  if (projectLoadingState.status === 'failed') {
    return <div className="field-padded"><Alert variant="error">Nie udało się pobrać projektu. {projectLoadingState.errorMessage}</Alert></div>;
  }
  if (projectId === null || projectLoadingState.status === 'loading' || mapDataState.status === 'loading') {
    return <div className="center field-padded"><LoadingSpinner /></div>;
  }
  if (mapDataState.status === 'failed') {
    return <div className="field-padded"><Alert variant="error">Nie udało się pobrać danych mapy. {mapDataState.errorMessage}</Alert></div>;
  }

  const selectedTree = trees.find((tree) => tree.id === selectedTreeId) ?? null;
  const isSheetOpen = selectedTreeId !== null;
  const isPickingOnMap = mapInteraction === 'PICKING_NEW_TREE' || mapInteraction === 'PICKING_TREE_LOCATION';
  const markingCatalogCodes = markingCatalog.catalogState.status === 'loaded'
    ? markingCatalog.catalogState.markings.map((marking) => marking.code) : [];
  const selectedTreeMarkingCodes = (projectMarkings ?? []).find((markings) => markings.treeId === selectedTreeId)?.codes ?? [];
  const treesWithoutLocationCount = trees.length - allTreeMapPoints.length;

  return (
    <div className="field-map-screen" data-sheet={isSheetOpen ? sheetSize : undefined}>
      <div className="field-map-area">
        <ProjectTreeMap treeMapPoints={allTreeMapPoints} symbolByCode={symbolByCode} viewMode={viewMode}
                        markingByCode={markingCatalog.markingByCode} stands={areStandsVisible ? stands : NO_STANDS}
                        referenceLayers={referenceLayers.layersOnMap} focusedTreeId={selectedTreeId}
                        onTreeMarkerClicked={isPickingOnMap ? () => {} : openTree}
                        onEmptyMapClicked={isPickingOnMap ? handleEmptyMapClick : undefined}
                        fillsContainer fitViewOnPointsChange="firstLoadOnly" selectedTreeId={selectedTreeId}
                        pendingLocation={pendingLocation} centerOn={centerRequest}
                        livePosition={lastReading ? { latitude: lastReading.latitude, longitude: lastReading.longitude,
                          accuracyM: lastReading.accuracyM } : null} />

        {mapInteraction === 'PICKING_NEW_TREE' && (
          <div className="field-map-hint" role="status">
            <span>Dotknij mapy w miejscu nowego drzewa.</span>
            <button type="button" className="btn btn-secondary" onClick={stopMapInteraction}>Anuluj</button>
          </div>
        )}
        {mapInteraction === 'PICKING_TREE_LOCATION' && (
          <div className="field-map-hint" role="status">
            <span>Dotknij mapy w nowym miejscu drzewa, potem „Zapisz” w panelu.</span>
          </div>
        )}
        {mapInteraction === 'NONE' && !isSheetOpen && trees.length > 0 && treesWithoutLocationCount > 0 && (
          <p className="field-map-message muted small card">Drzewa bez lokalizacji: {treesWithoutLocationCount} (niewidoczne na mapie).</p>
        )}

        {mapInteraction !== 'MEASURING_NEW_TREE' && (
          <div className="field-fabs">
            <button type="button" className="field-fab" onClick={() => setIsLayersSheetOpen((isOpen) => !isOpen)}
                    aria-pressed={isLayersSheetOpen}>Warstwy</button>
            <button type="button" className="field-fab" onClick={centerOnMyPosition} disabled={!lastReading}
                    title={lastReading ? undefined : 'Brak bieżącej pozycji urządzenia'}>Moja pozycja</button>
            <button type="button" className="field-fab" onClick={startPickingNewTree}
                    aria-pressed={mapInteraction === 'PICKING_NEW_TREE'}>Wskaż na mapie</button>
            <button type="button" className="field-fab field-fab-primary" onClick={startTreeHere}>+ Drzewo tutaj</button>
          </div>
        )}

        {mapInteraction === 'MEASURING_NEW_TREE' && (
          <div className="field-overlay" role="dialog" aria-label="Pomiar GPS nowego drzewa">
            <div className="field-overlay-header"><h2>Nowe drzewo - pomiar GPS</h2></div>
            <TreeGnssLocationPanel projectId={projectId} tree={null} describesForField onReadingConfirmed={confirmMeasuredNewTree}
                                   onPendingLocationChanged={setPendingLocation} onTreeLocationSaved={() => {}}
                                   initialMeasurementMode={measurementMode} onMeasurementModeChange={changeMeasurementMode}
                                   cancel={stopMapInteraction} />
          </div>
        )}

        {isLayersSheetOpen && (
          <FieldLayersSheet viewMode={viewMode} onViewModeChange={changeViewMode} standCount={stands.length}
                            areStandsVisible={areStandsVisible} onStandsVisibilityChange={setAreStandsVisible}
                            referenceLayersState={referenceLayers.state} hiddenReferenceLayerIds={referenceLayers.hiddenLayerIds}
                            toggleReferenceLayer={referenceLayers.toggleLayer} closeSheet={() => setIsLayersSheetOpen(false)}
                            legend={<ProjectMapModeLegend viewMode={viewMode} treeMapPoints={allTreeMapPoints}
                                                          allTreeMapPoints={allTreeMapPoints} legendEntries={legendEntries}
                                                          markingCatalogCodes={markingCatalogCodes}
                                                          markingByCode={markingCatalog.markingByCode} projectId={projectId} />}>
            <FieldOfflinePreparation projectId={projectId} trees={trees} />
          </FieldLayersSheet>
        )}
      </div>

      {isSheetOpen && selectedTreeId !== null && (
        <FieldTreeSheet projectId={projectId} treeId={selectedTreeId} listedTree={selectedTree}
                        markingCodes={selectedTreeMarkingCodes} markingByCode={markingCatalog.markingByCode}
                        size={sheetSize} onSizeChange={setSheetSize} onTreeChanged={upsertTree}
                        onTreeIdReplaced={replaceSelectedTreeId} onMarkingsSaved={onMarkingsSaved} onWorksChanged={onWorksChanged}
                        onPendingLocationChanged={setPendingLocation}
                        pickLocationOnMap={() => { setPendingLocation(null); setMapInteraction('PICKING_TREE_LOCATION'); }}
                        stopPickingOnMap={stopMapInteraction}
                        pickedLocation={mapInteraction === 'PICKING_TREE_LOCATION' ? pendingLocation : null}
                        measurementMode={measurementMode} onMeasurementModeChange={changeMeasurementMode}
                        onMeasuringChange={setIsMeasuringInSheet} closeSheet={closeSheet} startNextTree={startTreeHere} />
      )}

      {newTreeLocation && (
        <NewTreeDialog projectId={projectId} initialLocation={newTreeLocation} onTreeCreated={showCreatedTree}
                       suggestedInventoryNumber={suggestNextInventoryNumber(trees.map((tree) => tree.inventoryNumber))}
                       closeDialog={() => { setNewTreeLocation(null); setPendingLocation(null); }} />
      )}
      {unsavedChangesDialog}
    </div>
  );
}

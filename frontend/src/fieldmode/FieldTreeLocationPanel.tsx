import { useEffect, useRef, useState } from 'react';
import { ApiRequestError } from '../api/httpClient';
import { Alert } from '../components/Alert';
import { formatTreeLocationForDisplay } from '../pages/trees/location/formatTreeLocationForDisplay';
import { PolishGridCoordinatesText } from '../pages/trees/location/PolishGridCoordinatesText';
import { TreeGnssLocationPanel, type MeasurementMode } from '../pages/trees/location/TreeGnssLocationPanel';
import { isTreeLocationWithinValidRange } from '../pages/trees/location/treeLocationValidRange';
import type { Tree, TreeLocation } from '../pages/trees/treeDataTypes';
import { treesApi } from '../pages/trees/treesApi';
import { describeFieldLocation } from './fieldLocationDescription';

type LocationMode = 'viewing' | 'gnss' | 'moving';

/**
 * Lokalizacja drzewa w panelu terenowym (etap 32) - bez drugiej mapy w panelu: opis punktu (zrodlo, dokladnosc
 * zgloszona przez urzadzenie, PL-2000), "Zmierz ponownie (GPS)" z podgladem na glownej mapie (pendingLocation)
 * i "Przesun na mapie" - nastepne dotkniecie glownej mapy wskazuje punkt, potem "Zapisz" albo "Anuluj".
 * Ten sam endpoint (PUT .../location) co karta biurowa, takze offline (kolejka).
 */
export function FieldTreeLocationPanel({ projectId, tree, onTreeChanged, onPendingLocationChanged, pickLocationOnMap,
                                         stopPickingOnMap, pickedLocation, measurementMode, onMeasurementModeChange,
                                         onMeasuringChange }: {
  projectId: number;
  tree: Tree;
  onTreeChanged: (tree: Tree) => void;
  onPendingLocationChanged: (location: TreeLocation | null) => void;
  /** wlacza wskazywanie punktu na glownej mapie */
  pickLocationOnMap: () => void;
  stopPickingOnMap: () => void;
  /** punkt wskazany na glownej mapie (null = jeszcze nie wskazano) */
  pickedLocation: TreeLocation | null;
  measurementMode: MeasurementMode;
  onMeasurementModeChange: (mode: MeasurementMode) => void;
  /** trwa pomiar GPS - rodzic wstrzymuje wtedy sledzenie pozycji na zywo (patrz FieldProjectPage) */
  onMeasuringChange: (isMeasuring: boolean) => void;
}) {
  const [mode, setMode] = useState<LocationMode>('viewing');
  useEffect(() => {
    onMeasuringChange(mode === 'gnss');
    return () => onMeasuringChange(false);
  }, [mode, onMeasuringChange]);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const isSubmissionInProgress = useRef(false);
  const savedLocation = tree.location ?? null;

  const backToViewing = () => {
    setMode('viewing');
    setErrorMessage(null);
    onPendingLocationChanged(null);
    stopPickingOnMap();
  };

  const startMoving = () => {
    setMode('moving');
    setErrorMessage(null);
    pickLocationOnMap();
  };

  const savePickedLocation = async () => {
    if (!pickedLocation || isSubmissionInProgress.current) return;
    if (!isTreeLocationWithinValidRange(pickedLocation)) {
      setErrorMessage('Wybrany punkt ma nieprawidłowe współrzędne. Wskaż punkt ponownie.');
      return;
    }
    isSubmissionInProgress.current = true;
    setIsSaving(true);
    try {
      onTreeChanged(await treesApi.setTreeLocation(projectId, tree.id,
        { latitude: pickedLocation.latitude, longitude: pickedLocation.longitude, source: 'MAP' }));
      backToViewing();
    } catch (savingError) {
      setErrorMessage(savingError instanceof ApiRequestError
        ? Object.values(savingError.errorMessageByFieldName).join(' ') || savingError.message : 'Nieoczekiwany błąd.');
    } finally {
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  return (
    <section className="tree-location-section" aria-labelledby="field-tree-location-heading">
      <h3 id="field-tree-location-heading">Lokalizacja</h3>
      {savedLocation ? (
        <p className="field-location-summary">
          <span>{formatTreeLocationForDisplay(savedLocation)}</span>
          <PolishGridCoordinatesText location={savedLocation} />
          <span className="muted small">{describeFieldLocation(savedLocation)}</span>
        </p>
      ) : (
        <p className="muted">Lokalizacja drzewa nie jest jeszcze ustalona.</p>
      )}

      {mode === 'viewing' && (
        <div className="form-actions">
          <button type="button" className="btn btn-secondary" onClick={() => setMode('gnss')}>
            {savedLocation ? 'Zmierz ponownie (GPS)' : 'Zmierz (GPS)'}
          </button>
          <button type="button" className="btn btn-secondary" onClick={startMoving}>
            {savedLocation ? 'Przesuń na mapie' : 'Wskaż na mapie'}
          </button>
        </div>
      )}

      {mode === 'gnss' && (
        <TreeGnssLocationPanel projectId={projectId} tree={tree} describesForField onPendingLocationChanged={onPendingLocationChanged}
                               initialMeasurementMode={measurementMode} onMeasurementModeChange={onMeasurementModeChange}
                               onTreeLocationSaved={(savedTree) => { onTreeChanged(savedTree); backToViewing(); }}
                               cancel={backToViewing} />
      )}

      {mode === 'moving' && (
        <div className="map-picking-panel stack">
          {errorMessage && <Alert variant="error">{errorMessage}</Alert>}
          <p>{pickedLocation
            ? <>Nowy punkt: <strong>{formatTreeLocationForDisplay(pickedLocation)}</strong> - dotknij mapy ponownie, aby go poprawić.</>
            : <strong>Dotknij mapy w miejscu drzewa.</strong>}</p>
          <div className="form-actions">
            <button type="button" className="btn btn-ghost" onClick={backToViewing} disabled={isSaving}>Anuluj</button>
            <button type="button" className="btn btn-primary" onClick={savePickedLocation} disabled={!pickedLocation || isSaving}>
              {isSaving ? 'Zapisywanie…' : 'Zapisz'}
            </button>
          </div>
        </div>
      )}
    </section>
  );
}

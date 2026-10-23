import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiRequestError } from '../../../api/httpClient';
import { describeLocationMeasurement } from '../../../gnss/deviceGnss';
import { Alert } from '../../../components/Alert';
import { ToBeClarifiedNote } from '../../../components/ToBeClarifiedNote';
import { BasemapControls } from '../../../map/BasemapControls';
import type { Tree, TreeLocation } from '../treeDataTypes';
import { treesApi } from '../treesApi';
import { formatTreeLocationForDisplay } from './formatTreeLocationForDisplay';
import { PolishGridCoordinatesText } from './PolishGridCoordinatesText';
import { RemoveTreeLocationDialog } from './RemoveTreeLocationDialog';
import { TreeGnssLocationPanel } from './TreeGnssLocationPanel';
import { TreeLocationForm } from './TreeLocationForm';
import { TreeLocationMap } from './TreeLocationMap';
import { isTreeLocationWithinValidRange } from './treeLocationValidRange';

/** Co robi sekcja: tylko pokazuje punkt, edytuje go recznym formularzem, wskazaniem na mapie albo pomiarem GPS. */
type LocationEditingMode = 'viewing' | 'manualForm' | 'mapPicking' | 'gnss';

/**
 * Sekcja "Lokalizacja" karty drzewa: zapisany punkt (tekst + mapa, z opisem pomiaru) i operacje na tym samym
 * TreeLocation - pomiar GPS urzadzenia (etap 25), wskazanie na mapie, reczne wspolrzedne (formularz) i usuniecie. Mapa i formularz uzywaja tego samego endpointu
 * (PUT .../location) i tego samego drzewa z TreePage (jedno zrodlo prawdy), wiec zapis jednym od razu widac w drugim.
 * Wybor na mapie: "Ustaw/Zmien na mapie" -> klikniecie ustawia tymczasowy punkt -> "Zapisz lokalizacje" / "Anuluj".
 * Dopiero zapis wysyla request; po sukcesie pokazujemy punkt z odpowiedzi backendu (moze go zaokraglic).
 */
export function TreeLocationSection({ projectId, tree, onTreeChanged }: {
  projectId: number;
  tree: Tree;
  onTreeChanged: (changedTree: Tree) => void;
}) {
  const [editingMode, setEditingMode] = useState<LocationEditingMode>('viewing');
  const [pendingMapLocation, setPendingMapLocation] = useState<TreeLocation | null>(null);
  const [isSavingMapLocation, setIsSavingMapLocation] = useState(false);
  const [mapSavingErrorMessage, setMapSavingErrorMessage] = useState<string | null>(null);
  const [isRemovalDialogOpen, setIsRemovalDialogOpen] = useState(false);
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isMapLocationSubmissionInProgress = useRef(false);

  const savedLocation = tree.location ?? null;

  const startMapPicking = () => {
    setPendingMapLocation(null);
    setMapSavingErrorMessage(null);
    setEditingMode('mapPicking');
  };

  // Anulowanie niczego nie wysyla - zostaje zapisany punkt z drzewa.
  const stopEditing = () => {
    setPendingMapLocation(null);
    setMapSavingErrorMessage(null);
    setEditingMode('viewing');
  };

  const replaceTreeAndStopEditing = (savedTree: Tree) => {
    onTreeChanged(savedTree);
    stopEditing();
  };

  const saveMapLocation = async () => {
    if (!pendingMapLocation || isMapLocationSubmissionInProgress.current) return;
    if (!isTreeLocationWithinValidRange(pendingMapLocation)) {
      setMapSavingErrorMessage('Wybrany punkt ma nieprawidłowe współrzędne. Wskaż punkt ponownie.');
      return;
    }
    isMapLocationSubmissionInProgress.current = true;
    setIsSavingMapLocation(true);
    setMapSavingErrorMessage(null);
    try {
      // Tymczasowy punkt zostaje na mapie do odpowiedzi; potem zastepuje go punkt zapisany przez backend.
      replaceTreeAndStopEditing(await treesApi.setTreeLocation(projectId, tree.id,
        { latitude: pendingMapLocation.latitude, longitude: pendingMapLocation.longitude, source: 'MAP' }));
    } catch (savingError) {
      // Blad nie kasuje wskazanego punktu - mozna sprobowac ponownie albo wskazac inny.
      setMapSavingErrorMessage(savingError instanceof ApiRequestError
        ? Object.values(savingError.errorMessageByFieldName).join(' ') || savingError.message
        : 'Nieoczekiwany błąd.');
    } finally {
      isMapLocationSubmissionInProgress.current = false;
      setIsSavingMapLocation(false);
    }
  };

  return (
    <section className="tree-location-section" aria-labelledby="tree-location-heading">
      <div className="section-header-row">
        <h3 id="tree-location-heading">Lokalizacja</h3>
        {editingMode === 'viewing' && (
          <div className="form-actions">
            <button className={`btn ${savedLocation ? 'btn-secondary' : 'btn-primary'}`} onClick={startMapPicking}>
              {savedLocation ? 'Zmień na mapie' : 'Ustaw na mapie'}
            </button>
            <button className="btn btn-secondary" onClick={() => { setPendingMapLocation(null); setEditingMode('gnss'); }}>
              Użyj GPS urządzenia
            </button>
            <button className="btn btn-secondary" onClick={() => setEditingMode('manualForm')}>Wpisz współrzędne</button>
            {savedLocation && (
              <Link className="btn btn-secondary" to={`/projects/${projectId}/map?drzewo=${tree.id}`}>Pokaż na mapie projektu</Link>
            )}
            {savedLocation && (
              <button className="btn btn-danger-outline" onClick={() => setIsRemovalDialogOpen(true)}>Usuń</button>
            )}
          </div>
        )}
      </div>

      {editingMode !== 'manualForm' && (savedLocation ? (
        <p className="tree-location-coordinates">
          <span className="muted small">Współrzędne (WGS84): </span>{formatTreeLocationForDisplay(savedLocation)}
          <PolishGridCoordinatesText location={savedLocation} />
          {describeLocationMeasurement(savedLocation) && (
            <span className="tree-location-measurement muted small">{describeLocationMeasurement(savedLocation)}</span>
          )}
        </p>
      ) : editingMode === 'viewing' && (
        <p className="muted">
          Lokalizacja drzewa nie została jeszcze ustalona. Wybierz „Ustaw na mapie” i kliknij mapę, aby wskazać
          lokalizację drzewa, albo wpisz współrzędne ręcznie.
        </p>
      ))}

      {editingMode === 'manualForm' && (
        <TreeLocationForm projectId={projectId} tree={tree} onTreeLocationSaved={replaceTreeAndStopEditing}
                          cancelEditing={stopEditing} />
      )}

      {editingMode === 'gnss' && (
        <TreeGnssLocationPanel projectId={projectId} tree={tree} onPendingLocationChanged={setPendingMapLocation}
                               onTreeLocationSaved={replaceTreeAndStopEditing} cancel={stopEditing} />
      )}

      {(editingMode === 'viewing' || editingMode === 'gnss') && (
        <details className="gnss-external-receiver">
          <summary>Odbiornik GNSS / RTK (dokładność centymetrowa)</summary>
          <p className="muted small">
            Na Androidzie: połącz odbiornik w jego aplikacji i włącz w niej udostępnianie pozycji jako lokalizacji
            telefonu („pozorowana lokalizacja” w opcjach programisty). Wtedy „Użyj GPS urządzenia” pobierze pozycję
            z odbiornika - wybierz „Uśrednij” i sprawdź, czy dokładność jest oznaczona jako centymetrowa. Aplikacja nie
            wie, czy odbiornik ma rozwiązanie FIX - dokładność podaje urządzenie.
          </p>
          <ToBeClarifiedNote>
            Model odbiornika arborysty, korekty RTK (ASG-EUPOS / NTRIP), praca na iPhonie/iPadzie oraz zapis typu
            rozwiązania (FIX / FLOAT), PDOP i liczby satelitów - przeglądarka tych danych nie podaje.
          </ToBeClarifiedNote>
        </details>
      )}

      {editingMode === 'mapPicking' && (
        <div className="map-picking-panel stack">
          {mapSavingErrorMessage && <Alert variant="error">{mapSavingErrorMessage}</Alert>}
          {pendingMapLocation ? (
            <p>
              Wybrany punkt: <strong>{formatTreeLocationForDisplay(pendingMapLocation)}</strong>
              <span className="muted small"> — kliknij mapę ponownie, aby go poprawić.</span>
            </p>
          ) : (
            <p><strong>Kliknij mapę, aby wskazać lokalizację drzewa.</strong></p>
          )}
          <div className="form-actions">
            {/* Anuluj zablokowane w trakcie zapisu - inaczej odpowiedz PUT nadpisalaby porzucona edycje. */}
            <button type="button" className="btn btn-ghost" onClick={stopEditing} disabled={isSavingMapLocation}>
              Anuluj
            </button>
            <button type="button" className="btn btn-primary" onClick={saveMapLocation}
                    disabled={!pendingMapLocation || isSavingMapLocation}>
              {isSavingMapLocation ? 'Zapisywanie…' : 'Zapisz lokalizację'}
            </button>
          </div>
        </div>
      )}

      {/* Mapa zamontowana stale (jedna instancja OpenLayers) - zmienia sie tylko to, co pokazuje. */}
      <TreeLocationMap savedLocation={savedLocation}
                       pendingLocation={editingMode === 'mapPicking' || editingMode === 'gnss' ? pendingMapLocation : null}
                       isPickingLocation={editingMode === 'mapPicking' && !isSavingMapLocation}
                       onLocationPicked={setPendingMapLocation} />
      <BasemapControls />

      {isRemovalDialogOpen && (
        <RemoveTreeLocationDialog projectId={projectId} treeId={tree.id}
                                  onTreeLocationRemoved={(changedTree) => {
                                    onTreeChanged(changedTree);
                                    setIsRemovalDialogOpen(false);
                                  }}
                                  closeDialog={() => setIsRemovalDialogOpen(false)} />
      )}
    </section>
  );
}

import type { ReactNode } from 'react';
import { ToBeClarifiedNote } from '../components/ToBeClarifiedNote';
import { BasemapControls } from '../map/BasemapControls';
import type { ReferenceLayersState } from '../pages/projects/referencelayers/useProjectReferenceLayers';
import { MAP_VIEW_MODES, type MapViewMode } from '../pages/trees/map/mapViewMode';

/**
 * Arkusz "Warstwy" trybu terenowego (etap 32): tryb widoku mapy, podklad, grupy drzew i warstwy referencyjne
 * (tylko widocznosc - wgrywanie plikow zostaje w biurze), legenda biezacego trybu oraz przygotowanie projektu do pracy
 * bez zasiegu (children). Jedna zwijana notatka "Do doprecyzowania" zamiast notatek rozsianych po ekranie.
 */
export function FieldLayersSheet({ viewMode, onViewModeChange, standCount, areStandsVisible, onStandsVisibilityChange,
                                   referenceLayersState, hiddenReferenceLayerIds, toggleReferenceLayer, legend, children,
                                   closeSheet }: {
  viewMode: MapViewMode;
  onViewModeChange: (mode: MapViewMode) => void;
  standCount: number;
  areStandsVisible: boolean;
  onStandsVisibilityChange: (isVisible: boolean) => void;
  referenceLayersState: ReferenceLayersState;
  hiddenReferenceLayerIds: Set<number>;
  toggleReferenceLayer: (layerId: number) => void;
  legend: ReactNode;
  children?: ReactNode;
  closeSheet: () => void;
}) {
  return (
    <div className="field-overlay" role="dialog" aria-label="Warstwy mapy">
      <div className="field-overlay-header">
        <h2>Warstwy</h2>
        <button type="button" className="btn btn-secondary" onClick={closeSheet}>Zamknij</button>
      </div>

      <section aria-label="Widok mapy">
        <h3>Widok mapy</h3>
        <div className="map-view-mode" role="radiogroup" aria-label="Widok mapy">
          {MAP_VIEW_MODES.map(({ mode, label }) => (
            <button key={mode} type="button" role="radio" aria-checked={viewMode === mode}
                    className={`btn btn-small ${viewMode === mode ? 'btn-primary' : 'btn-secondary'}`} onClick={() => onViewModeChange(mode)}>
              {label}
            </button>
          ))}
        </div>
      </section>

      <section aria-label="Podkład mapy">
        <h3>Podkład</h3>
        <BasemapControls />
      </section>

      {(standCount > 0 || (referenceLayersState.status === 'loaded' && referenceLayersState.layers.length > 0)) && (
        <section aria-label="Warstwy projektu">
          <h3>Warstwy projektu</h3>
          {standCount > 0 && (
            <label className="checkbox-row">
              <input type="checkbox" checked={areStandsVisible} onChange={(changeEvent) => onStandsVisibilityChange(changeEvent.target.checked)} />
              <span>Grupy drzew ({standCount})</span>
            </label>
          )}
          {referenceLayersState.status === 'loaded' && referenceLayersState.layers.map((layer) => (
            <label key={layer.id} className="checkbox-row">
              <input type="checkbox" checked={!hiddenReferenceLayerIds.has(layer.id)} onChange={() => toggleReferenceLayer(layer.id)} />
              <span>{layer.name} <span className="muted small">({layer.featureCount} obiektów)</span></span>
            </label>
          ))}
          <p className="muted small">Nowe warstwy (pliki z QGIS) dodaje się w widoku biurowym.</p>
        </section>
      )}
      {referenceLayersState.status === 'failed' && (
        <p className="muted small">Nie udało się pobrać warstw referencyjnych projektu.</p>
      )}

      <section aria-label="Legenda">{legend}</section>

      {children}

      <details>
        <summary className="muted small">Do doprecyzowania w trybie terenowym</summary>
        <ToBeClarifiedNote>
          Układ trybu terenowego to wersja V1 do sprawdzenia na tablecie: kolejność sekcji w panelu drzewa, domyślny
          widok „Oznaczenia drzew”, proponowany numer drzewa (największy + 1), pomiar pojedynczy domyślnie. Podkład mapy
          i zdjęcia nie działają bez zasięgu. Przeglądarka nie podaje typu rozwiązania odbiornika (FIX/FLOAT) - pokazujemy
          tylko dokładność zgłoszoną przez urządzenie.
        </ToBeClarifiedNote>
      </details>
    </div>
  );
}

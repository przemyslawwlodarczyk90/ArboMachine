import { useId, useSyncExternalStore } from 'react';
import { ToBeClarifiedNote } from '../components/ToBeClarifiedNote';
import { BASEMAP_OPTIONS, basemapPreferenceStore, type BasemapId } from './basemapPreference';

/**
 * Wybor podkladu mapy i nakladki dzialek (etap 31) - pod kazda mapa. Wybor jest wspolny dla wszystkich map
 * i zapamietany na urzadzeniu; mapy same go sledza (createMapWithBasemap), wiec komponent niczego mapie nie przekazuje.
 */
export function BasemapControls({ showsToBeClarifiedNotes = true }: {
  /** etap 32: w trybie terenowym notatki sa zebrane w jednej zwijanej notatce arkusza "Warstwy" */
  showsToBeClarifiedNotes?: boolean;
} = {}) {
  const selection = useSyncExternalStore(basemapPreferenceStore.subscribe, basemapPreferenceStore.getSelection);
  const selectId = useId();
  const unavailableOptions = BASEMAP_OPTIONS.filter((option) => !option.isAvailable);

  return (
    <div className="basemap-controls">
      <div className="basemap-controls-row">
        <label htmlFor={selectId} className="muted small">Podkład:</label>
        <select id={selectId} className="select" value={selection.basemapId}
                onChange={(changeEvent) => basemapPreferenceStore.update({ basemapId: changeEvent.target.value as BasemapId })}>
          {BASEMAP_OPTIONS.map((option) => (
            <option key={option.id} value={option.id} disabled={!option.isAvailable}>
              {option.label}{option.isAvailable ? '' : ' (do doprecyzowania)'}
            </option>
          ))}
        </select>
        <label className="checkbox-row">
          <input type="checkbox" checked={selection.showParcels}
                 onChange={(changeEvent) => basemapPreferenceStore.update({ showParcels: changeEvent.target.checked })} />
          <span>Działki ewidencyjne <span className="muted small">(po przybliżeniu)</span></span>
        </label>
      </div>
      {showsToBeClarifiedNotes && unavailableOptions.map((option) => (
        <ToBeClarifiedNote key={option.id}>{option.toBeClarifiedNote}</ToBeClarifiedNote>
      ))}
      {showsToBeClarifiedNotes && <ToBeClarifiedNote>
        Podkłady Geoportalu wymagają internetu (nie są zapisywane offline). Mapa zasadnicza / BDOT10k i własne podkłady
        wektorowe z QGIS - które warstwy są potrzebne w terenie?
      </ToBeClarifiedNote>}
    </div>
  );
}

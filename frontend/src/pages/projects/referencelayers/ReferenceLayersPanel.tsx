import { useId, useRef, useState, type FormEvent } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { ToBeClarifiedNote } from '../../../components/ToBeClarifiedNote';
import type { ReferenceLayer } from './referenceLayerDataTypes';
import { describeRejectedLayerFile, referenceLayerColorOf } from './referenceLayerPresentation';
import { referenceLayersApi } from './referenceLayersApi';
import type { ReferenceLayersState } from './useProjectReferenceLayers';

const FORMAT_LABELS: Record<string, string> = { GEOJSON: 'GeoJSON', SHAPEFILE: 'Shapefile', GEOPACKAGE: 'GeoPackage', DXF: 'DXF' };

/**
 * Panel "Warstwy referencyjne (QGIS)" pod mapa projektu (etap 31): lista warstw z kolorem i przelacznikiem widocznosci,
 * dodanie pliku (GeoJSON / Shapefile ZIP / GeoPackage / DXF) i usuniecie. Stan warstw trzyma useProjectReferenceLayers.
 */
export function ReferenceLayersPanel({ projectId, state, hiddenLayerIds, toggleLayer, onLayerAdded, onLayerRemoved }: {
  projectId: number;
  state: ReferenceLayersState;
  hiddenLayerIds: Set<number>;
  toggleLayer: (layerId: number) => void;
  onLayerAdded: (layer: ReferenceLayer) => void;
  onLayerRemoved: (layerId: number) => void;
}) {
  const [file, setFile] = useState<File | null>(null);
  const [name, setName] = useState('');
  const [isWorking, setIsWorking] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const fileInputId = useId();
  const nameInputId = useId();

  const uploadLayer = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (!file || isWorking) return;
    setIsWorking(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      const layer = await referenceLayersApi.uploadLayer(projectId, file, name);
      onLayerAdded(layer);
      setSuccessMessage(`Dodano warstwę „${layer.name}” (obiekty: ${layer.featureCount}).`);
      setFile(null);
      setName('');
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (uploadError) {
      setErrorMessage(uploadError instanceof ApiRequestError
        ? (uploadError.errorMessageByFieldName.code
          ? describeRejectedLayerFile(uploadError.errorMessageByFieldName.code, uploadError.errorMessageByFieldName.detail)
          : uploadError.message)
        : 'Nieoczekiwany błąd.');
    } finally {
      setIsWorking(false);
    }
  };

  const deleteLayer = async (layer: ReferenceLayer) => {
    if (isWorking || !window.confirm(`Usunąć warstwę „${layer.name}”? Drzewa i inne dane projektu zostaną bez zmian.`)) return;
    setIsWorking(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await referenceLayersApi.deleteLayer(projectId, layer.id);
      onLayerRemoved(layer.id);
    } catch (deletionError) {
      setErrorMessage(`Nie udało się usunąć warstwy. ${deletionError instanceof ApiRequestError ? deletionError.message : ''}`);
    } finally {
      setIsWorking(false);
    }
  };

  return (
    <details className="reference-layers-panel">
      <summary>Warstwy referencyjne z QGIS {state.status === 'loaded' && `(${state.layers.length})`}</summary>
      <p className="muted small">
        Własny podkład wektorowy projektu: granice, działki, drogi, sieci, wcześniejsza inwentaryzacja. Warstwa jest
        tylko do podglądu pod drzewami - nie tworzy drzew (do tego służy „Import i eksport”).
      </p>
      {state.status === 'failed' && <Alert variant="error">Nie udało się pobrać warstw. {state.errorMessage}</Alert>}
      {errorMessage && <Alert variant="error">{errorMessage}</Alert>}
      {successMessage && <Alert variant="success">{successMessage}</Alert>}
      {state.status === 'loaded' && state.layers.length > 0 && (
        <ul className="reference-layer-list">
          {state.layers.map((layer, index) => (
            <li key={layer.id}>
              <label className="checkbox-row">
                <input type="checkbox" checked={!hiddenLayerIds.has(layer.id)} onChange={() => toggleLayer(layer.id)} />
                <span className="reference-layer-swatch" style={{ borderColor: referenceLayerColorOf(index) }} aria-hidden="true" />
                <span>
                  <strong>{layer.name}</strong>
                  <span className="muted small"> · {FORMAT_LABELS[layer.sourceFormat] ?? layer.sourceFormat} · obiekty: {layer.featureCount}
                    {layer.sourceCrs ? ` · ${layer.sourceCrs}` : ''}</span>
                </span>
              </label>
              <button type="button" className="btn btn-ghost btn-small" disabled={isWorking} onClick={() => deleteLayer(layer)}>Usuń</button>
            </li>
          ))}
        </ul>
      )}
      <form className="reference-layer-upload" onSubmit={uploadLayer}>
        <div className="field">
          <label htmlFor={fileInputId}>Plik warstwy</label>
          <input id={fileInputId} ref={fileInputRef} type="file" accept=".geojson,.json,.zip,.gpkg,.dxf" disabled={isWorking}
                 onChange={(changeEvent) => setFile(changeEvent.target.files?.[0] ?? null)} />
        </div>
        <div className="field">
          <label htmlFor={nameInputId}>Nazwa (opcjonalnie)</label>
          <input id={nameInputId} value={name} maxLength={255} disabled={isWorking} placeholder="np. Działki ewidencyjne"
                 onChange={(changeEvent) => setName(changeEvent.target.value)} />
        </div>
        <div className="form-actions">
          <button type="submit" className="btn btn-secondary" disabled={!file || isWorking}>
            {isWorking ? 'Wgrywanie…' : 'Dodaj warstwę'}
          </button>
        </div>
      </form>
      <ToBeClarifiedNote>
        Czy warstwy z QGIS mają być tylko podglądem, czy także edytowalne w terenie? Czy potrzebny jest styl z QGIS
        (kolory, symbole), opisy z pliku na mapie i praca offline? Przykładowy projekt QGIS arborysty pozwoli to dobrać.
      </ToBeClarifiedNote>
    </details>
  );
}

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import type { ReferenceLayer, ReferenceLayerOnMap } from './referenceLayerDataTypes';
import { referenceLayerColorOf } from './referenceLayerPresentation';
import { referenceLayersApi } from './referenceLayersApi';

export type ReferenceLayersState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; layers: ReferenceLayer[] };

/**
 * Warstwy referencyjne projektu dla mapy (etap 31): lista, widocznosc (domyslnie wszystkie widoczne, tylko w pamieci
 * strony), geometria pobierana raz na warstwe i dopiero, gdy warstwa jest widoczna.
 */
export function useProjectReferenceLayers(projectId: number) {
  const [state, setState] = useState<ReferenceLayersState>({ status: 'loading' });
  const [hiddenLayerIds, setHiddenLayerIds] = useState<Set<number>>(new Set());
  const [geoJsonByLayerId, setGeoJsonByLayerId] = useState<Map<number, unknown>>(new Map());
  // Warstwy, o ktorych geometrie juz zapytalismy - bez ponownych zapytan przy kolejnych renderach.
  const requestedLayerIdsRef = useRef(new Set<number>());

  const reload = useCallback(() => {
    let isResponseStillWanted = true;
    referenceLayersApi.listLayers(projectId)
      .then((layers) => { if (isResponseStillWanted) setState({ status: 'loaded', layers }); })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setState({ status: 'failed', errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId]);

  useEffect(() => reload(), [reload]);

  const layers = useMemo(() => (state.status === 'loaded' ? state.layers : []), [state]);

  // Geometria widocznych warstw, o ktore jeszcze nie pytalismy (blad = mozna sprobowac ponownie po przelaczeniu).
  useEffect(() => {
    for (const layer of layers) {
      if (hiddenLayerIds.has(layer.id) || requestedLayerIdsRef.current.has(layer.id)) continue;
      requestedLayerIdsRef.current.add(layer.id);
      referenceLayersApi.getLayerGeoJson(projectId, layer.id)
        .then((geoJson) => setGeoJsonByLayerId((current) => new Map(current).set(layer.id, geoJson)))
        .catch(() => { requestedLayerIdsRef.current.delete(layer.id); });
    }
  }, [projectId, layers, hiddenLayerIds]);

  const layersOnMap: ReferenceLayerOnMap[] = useMemo(() => layers.flatMap((layer, index) =>
    !hiddenLayerIds.has(layer.id) && geoJsonByLayerId.has(layer.id)
      ? [{ id: layer.id, color: referenceLayerColorOf(index), geoJson: geoJsonByLayerId.get(layer.id) }]
      : []), [layers, hiddenLayerIds, geoJsonByLayerId]);

  const toggleLayer = (layerId: number) => setHiddenLayerIds((current) => {
    const next = new Set(current);
    if (next.has(layerId)) next.delete(layerId); else next.add(layerId);
    return next;
  });

  const addLayer = (layer: ReferenceLayer) =>
    setState((current) => (current.status === 'loaded' ? { status: 'loaded', layers: [...current.layers, layer] } : current));

  const removeLayer = (layerId: number) =>
    setState((current) => (current.status === 'loaded'
      ? { status: 'loaded', layers: current.layers.filter((layer) => layer.id !== layerId) } : current));

  return { state, layers, hiddenLayerIds, layersOnMap, toggleLayer, addLayer, removeLayer };
}

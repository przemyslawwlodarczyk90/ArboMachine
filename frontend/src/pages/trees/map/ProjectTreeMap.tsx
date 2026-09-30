import { useEffect, useRef } from 'react';
import Feature from 'ol/Feature';
import type Map from 'ol/Map';
import Point from 'ol/geom/Point';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import {
  createMapWithOpenStreetMapBase, createTreeMarkerStyle, fitViewToCoordinates, MARKER_HIT_TOLERANCE_PX,
  SAVED_TREE_MARKER_COLOR,
} from '../../../map/openLayersBaseMap';
import type { TreeMapPoint } from './treeMapPoints';

/** Nazwy wlasciwosci Feature - po nich klikniety marker jest wiazany z konkretnym drzewem. */
const TREE_ID_FEATURE_PROPERTY = 'treeId';
const INVENTORY_NUMBER_FEATURE_PROPERTY = 'inventoryNumber';

/**
 * Mapa projektu (OpenLayers): podklad OSM + warstwa markerow drzew. Tylko podglad - nie edytuje lokalizacji.
 * Instancja mapy powstaje raz (przy montowaniu) i jest niszczona przy odmontowaniu; zmiana punktow podmienia
 * jedynie Feature w warstwie i dopasowuje widok. Klikniecie markera zglasza treeId (albo null - klik obok).
 */
export function ProjectTreeMap({ treeMapPoints, onTreeMarkerClicked }: {
  treeMapPoints: TreeMapPoint[];
  onTreeMarkerClicked: (treeId: number | null) => void;
}) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const treeMarkerSourceRef = useRef<VectorSource<Feature<Point>> | null>(null);
  // Najnowszy callback w refie - listener OpenLayers rejestrujemy raz i nie chcemy starego domkniecia.
  const onTreeMarkerClickedRef = useRef(onTreeMarkerClicked);
  onTreeMarkerClickedRef.current = onTreeMarkerClicked;

  useEffect(() => {
    if (!mapContainerRef.current) return;
    const treeMarkerSource = new VectorSource<Feature<Point>>();
    const map = createMapWithOpenStreetMapBase(mapContainerRef.current, [
      new VectorLayer({ source: treeMarkerSource, style: createTreeMarkerStyle(SAVED_TREE_MARKER_COLOR) }),
    ]);

    map.on('singleclick', (clickEvent) => {
      const clickedFeature = map.forEachFeatureAtPixel(clickEvent.pixel, (feature) => feature,
        { hitTolerance: MARKER_HIT_TOLERANCE_PX });
      const clickedTreeId = clickedFeature?.get(TREE_ID_FEATURE_PROPERTY);
      onTreeMarkerClickedRef.current(typeof clickedTreeId === 'number' ? clickedTreeId : null);
    });
    // Kursor "reki" nad markerem (desktop) - sygnal, ze marker jest klikalny.
    map.on('pointermove', (pointerEvent) => {
      if (pointerEvent.dragging) return;
      const isOverTreeMarker = map.hasFeatureAtPixel(pointerEvent.pixel, { hitTolerance: MARKER_HIT_TOLERANCE_PX });
      map.getTargetElement().style.cursor = isOverTreeMarker ? 'pointer' : '';
    });

    mapRef.current = map;
    treeMarkerSourceRef.current = treeMarkerSource;
    return () => {
      map.setTarget(undefined);
      map.dispose();
      mapRef.current = null;
      treeMarkerSourceRef.current = null;
    };
  }, []);

  useEffect(() => {
    const map = mapRef.current;
    const treeMarkerSource = treeMarkerSourceRef.current;
    if (!map || !treeMarkerSource) return;
    treeMarkerSource.clear();
    treeMarkerSource.addFeatures(treeMapPoints.map((treeMapPoint) => new Feature({
      geometry: new Point(treeMapPoint.mapCoordinate),
      [TREE_ID_FEATURE_PROPERTY]: treeMapPoint.treeId,
      [INVENTORY_NUMBER_FEATURE_PROPERTY]: treeMapPoint.inventoryNumber,
    })));
    fitViewToCoordinates(map, treeMapPoints.map((treeMapPoint) => treeMapPoint.mapCoordinate));
  }, [treeMapPoints]);

  return <div ref={mapContainerRef} className="map-container" role="region" aria-label="Mapa drzew projektu" />;
}

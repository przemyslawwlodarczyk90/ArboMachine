import { useEffect, useRef } from 'react';
import Feature from 'ol/Feature';
import type Map from 'ol/Map';
import Point from 'ol/geom/Point';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import type { FeatureLike } from 'ol/Feature';
import type Style from 'ol/style/Style';
import type { MapSymbol } from '../../../map/mapSymbolDataTypes';
import { layoutWorkBadges } from '../../../map/mapSymbolShapeSpec';
import { createMapSymbolStyles, createOverflowBadgeStyle } from '../../../map/mapSymbolStyleFactory';
import {
  createMapWithOpenStreetMapBase, createTreeMarkerStyle, fitViewToCoordinates, MARKER_HIT_TOLERANCE_PX,
  SAVED_TREE_MARKER_COLOR,
} from '../../../map/openLayersBaseMap';
import type { TreeMapPoint } from './treeMapPoints';

/** Nazwy wlasciwosci Feature - po nich klikniety marker jest wiazany z konkretnym drzewem. */
const TREE_ID_FEATURE_PROPERTY = 'treeId';
const INVENTORY_NUMBER_FEATURE_PROPERTY = 'inventoryNumber';
const TREE_SYMBOL_FEATURE_PROPERTY = 'treeSymbolCode';
const WORK_SYMBOLS_FEATURE_PROPERTY = 'workSymbolCodes';

/** Rozmiary V1 (piksele): centralny symbol drzewa i mniejsze znaczki prac wokol niego. */
const TREE_SYMBOL_RADIUS_PX = 8;
const WORK_BADGE_RADIUS_PX = 6;
const NEUTRAL_TREE_STYLE = createTreeMarkerStyle(SAVED_TREE_MARKER_COLOR);

/**
 * Style jednego drzewa: centralny marker (symbol drzewa albo neutralny) + znaczki symboli prac przesuniete
 * wizualnie wokol punktu (displacement - wspolrzedne drzewa sie nie zmieniaja) + "+N" przy nadmiarze.
 * Nieznany kod (spoza katalogu) = neutralny wyglad / pominiety znaczek zamiast bledu mapy.
 */
function buildTreeStyles(treeSymbolCode: string | null, workSymbolCodes: string[],
                         symbolByCode: globalThis.Map<string, MapSymbol>): Style[] {
  const treeSymbol = treeSymbolCode ? symbolByCode.get(treeSymbolCode) : undefined;
  const styles = treeSymbol ? createMapSymbolStyles(treeSymbol, { radius: TREE_SYMBOL_RADIUS_PX }) : [NEUTRAL_TREE_STYLE];
  const knownWorkSymbolCodes = workSymbolCodes.filter((symbolCode) => symbolByCode.has(symbolCode));
  const badgeLayout = layoutWorkBadges(knownWorkSymbolCodes);
  for (const badge of badgeLayout.badges) {
    styles.push(...createMapSymbolStyles(symbolByCode.get(badge.symbolCode)!,
      { radius: WORK_BADGE_RADIUS_PX, displacement: badge.displacement }));
  }
  if (badgeLayout.overflowCount > 0) {
    styles.push(createOverflowBadgeStyle(badgeLayout.overflowCount, badgeLayout.overflowDisplacement));
  }
  return styles;
}

/**
 * Mapa projektu (OpenLayers): podklad OSM + warstwa markerow drzew z symbolami drzew i prac (etap 19).
 * Tylko podglad - nie edytuje lokalizacji ani symboli.
 * Instancja mapy powstaje raz (przy montowaniu) i jest niszczona przy odmontowaniu; zmiana punktow podmienia
 * jedynie Feature w warstwie i dopasowuje widok. Klikniecie markera zglasza treeId (albo null - klik obok).
 */
export function ProjectTreeMap({ treeMapPoints, symbolByCode, onTreeMarkerClicked }: {
  treeMapPoints: TreeMapPoint[];
  symbolByCode: globalThis.Map<string, MapSymbol>;
  onTreeMarkerClicked: (treeId: number | null) => void;
}) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const treeMarkerSourceRef = useRef<VectorSource<Feature<Point>> | null>(null);
  // Najnowszy callback w refie - listener OpenLayers rejestrujemy raz i nie chcemy starego domkniecia.
  const onTreeMarkerClickedRef = useRef(onTreeMarkerClicked);
  onTreeMarkerClickedRef.current = onTreeMarkerClicked;
  // Katalog symboli w refie + pamiec stylow wg kombinacji symboli (wiele drzew ma te same symbole).
  const symbolByCodeRef = useRef(symbolByCode);
  const styleCacheRef = useRef(new globalThis.Map<string, Style[]>());
  const treeMarkerLayerRef = useRef<VectorLayer<VectorSource<Feature<Point>>> | null>(null);

  useEffect(() => {
    if (!mapContainerRef.current) return;
    const treeMarkerSource = new VectorSource<Feature<Point>>();
    const treeMarkerLayer = new VectorLayer({
      source: treeMarkerSource,
      style: (feature: FeatureLike) => {
        const treeSymbolCode = (feature.get(TREE_SYMBOL_FEATURE_PROPERTY) as string | null) ?? null;
        const workSymbolCodes = (feature.get(WORK_SYMBOLS_FEATURE_PROPERTY) as string[] | undefined) ?? [];
        const styleKey = `${treeSymbolCode}|${workSymbolCodes.join(',')}`;
        let styles = styleCacheRef.current.get(styleKey);
        if (!styles) {
          styles = buildTreeStyles(treeSymbolCode, workSymbolCodes, symbolByCodeRef.current);
          styleCacheRef.current.set(styleKey, styles);
        }
        return styles;
      },
    });
    const map = createMapWithOpenStreetMapBase(mapContainerRef.current, [treeMarkerLayer]);
    treeMarkerLayerRef.current = treeMarkerLayer;

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
      treeMarkerLayerRef.current = null;
    };
  }, []);

  // Katalog symboli dochodzi asynchronicznie - po jego zmianie przerysowujemy markery nowymi stylami.
  useEffect(() => {
    symbolByCodeRef.current = symbolByCode;
    styleCacheRef.current.clear();
    treeMarkerLayerRef.current?.changed();
  }, [symbolByCode]);

  useEffect(() => {
    const map = mapRef.current;
    const treeMarkerSource = treeMarkerSourceRef.current;
    if (!map || !treeMarkerSource) return;
    treeMarkerSource.clear();
    treeMarkerSource.addFeatures(treeMapPoints.map((treeMapPoint) => new Feature({
      geometry: new Point(treeMapPoint.mapCoordinate),
      [TREE_ID_FEATURE_PROPERTY]: treeMapPoint.treeId,
      [INVENTORY_NUMBER_FEATURE_PROPERTY]: treeMapPoint.inventoryNumber,
      [TREE_SYMBOL_FEATURE_PROPERTY]: treeMapPoint.treeSymbolCode,
      [WORK_SYMBOLS_FEATURE_PROPERTY]: treeMapPoint.workSymbolCodes,
    })));
    fitViewToCoordinates(map, treeMapPoints.map((treeMapPoint) => treeMapPoint.mapCoordinate));
  }, [treeMapPoints]);

  return <div ref={mapContainerRef} className="map-container" role="region" aria-label="Mapa drzew projektu" />;
}

import { useEffect, useRef } from 'react';
import Feature from 'ol/Feature';
import type Map from 'ol/Map';
import Point from 'ol/geom/Point';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import type { FeatureLike } from 'ol/Feature';
import type { MapSymbol } from '../../../map/mapSymbolDataTypes';
import { layoutWorkBadges } from '../../../map/mapSymbolShapeSpec';
import { createMapSymbolStyles, createOverflowBadgeStyle } from '../../../map/mapSymbolStyleFactory';
import {
  createMapWithBasemap, createTreeMarkerStyle, fitViewToCoordinates, MARKER_HIT_TOLERANCE_PX,
  PENDING_TREE_MARKER_COLOR, SAVED_TREE_MARKER_COLOR, TREE_FOCUS_ZOOM,
} from '../../../map/openLayersBaseMap';
import { valuationColorOf } from '../valuation/valuationColors';
import type { TreeValuationCategory } from '../valuation/valuationDataTypes';
import { managementColorOf, type TreeManagementSummary } from '../works/workManagement';
import type { MapViewMode } from './mapViewMode';
import type { TreeMapPoint } from './treeMapPoints';
import Polygon, { circular } from 'ol/geom/Polygon';
import { fromLonLat } from 'ol/proj';
import { toMapCoordinate } from '../location/treeLocationMapCoordinates';
import { Fill, Stroke, Style, Text } from 'ol/style';
import { type SpeciesGroup } from '../../../map/speciesGroup';
import { type TreeMarkerShape } from '../../../map/openLayersBaseMap';
import { standPolygonOf, type StandMapShape } from '../../projects/stands/standGeometry';
import { createTreeMarkingStyles, type MapMarkingAppearance } from '../../../map/treeMarkingStyleFactory';
import type { TreeLocation } from '../treeDataTypes';
import { toTreeLocation } from '../location/treeLocationMapCoordinates';
import GeoJSON from 'ol/format/GeoJSON';
import { Circle as CircleStyle } from 'ol/style';
import type { ReferenceLayerOnMap } from '../../projects/referencelayers/referenceLayerDataTypes';

/** Stala pusta lista warstw referencyjnych (domyslny props). */
const NO_REFERENCE_LAYERS: ReferenceLayerOnMap[] = [];
const REFERENCE_COLOR_PROPERTY = 'referenceLayerColor';

/** Warstwa referencyjna (etap 31): linie i obrysy w kolorze warstwy, obszary z lekkim wypelnieniem, punkty jako kolka. */
const referenceStyleCache = new globalThis.Map<string, Style>();
function referenceStyleOf(color: string): Style {
  let style = referenceStyleCache.get(color);
  if (!style) {
    style = new Style({
      stroke: new Stroke({ color, width: 2 }),
      fill: new Fill({ color: `${color}1f` }),
      image: new CircleStyle({ radius: 4, fill: new Fill({ color }), stroke: new Stroke({ color: '#ffffff', width: 1 }) }),
    });
    referenceStyleCache.set(color, style);
  }
  return style;
}

/** Pusta mapa oznaczen - domyslna wartosc propsa bez nowego obiektu przy kazdym renderze. */
const NO_MARKINGS = new globalThis.Map<string, MapMarkingAppearance>();

/** Etap 32 (tryb terenowy): biezaca pozycja uzytkownika - niebieska kropka z kolem dokladnosci zglaszanej przez urzadzenie. */
export interface LivePositionOnMap {
  latitude: number;
  longitude: number;
  accuracyM: number;
}

/** Etap 32: jednorazowe wysrodkowanie mapy; nowy requestId = nowe wysrodkowanie (takze na ten sam punkt). */
export interface MapCenterRequest {
  latitude: number;
  longitude: number;
  requestId: number;
}

const LIVE_POSITION_DOT_STYLE = new Style({
  image: new CircleStyle({ radius: 7, fill: new Fill({ color: '#1c7ed6' }), stroke: new Stroke({ color: '#ffffff', width: 3 }) }),
});
const LIVE_POSITION_ACCURACY_STYLE = new Style({
  fill: new Fill({ color: 'rgba(28, 126, 214, 0.15)' }), stroke: new Stroke({ color: 'rgba(28, 126, 214, 0.6)', width: 1 }),
});
const SELECTED_TREE_RING_STYLE = new Style({
  image: new CircleStyle({ radius: 17, stroke: new Stroke({ color: '#1d2419', width: 3 }), fill: new Fill({ color: 'rgba(255, 255, 255, 0.25)' }) }),
});
const PENDING_LOCATION_STYLE = createTreeMarkerStyle(PENDING_TREE_MARKER_COLOR);
const PENDING_ACCURACY_STYLE = new Style({
  fill: new Fill({ color: 'rgba(217, 72, 15, 0.12)' }), stroke: new Stroke({ color: 'rgba(217, 72, 15, 0.7)', width: 1, lineDash: [6, 4] }),
});
/** Kolo dokladnosci rysujemy tylko do 500 m - wieksze (lokalizacja z sieci) zaslonilyby cala mape. */
const MAXIMUM_DRAWN_ACCURACY_M = 500;

/** Kolo dokladnosci w metrach na ziemi (geodezyjnie), przeliczone do widoku mapy EPSG:3857. */
const accuracyCircleOf = (latitude: number, longitude: number, accuracyM: number) =>
  circular([longitude, latitude], Math.min(accuracyM, MAXIMUM_DRAWN_ACCURACY_M), 64).transform('EPSG:4326', 'EPSG:3857');

/** Nazwy wlasciwosci Feature - po nich klikniety marker jest wiazany z konkretnym drzewem. */
const TREE_ID_FEATURE_PROPERTY = 'treeId';
const INVENTORY_NUMBER_FEATURE_PROPERTY = 'inventoryNumber';
const TREE_SYMBOL_FEATURE_PROPERTY = 'treeSymbolCode';
const WORK_SYMBOLS_FEATURE_PROPERTY = 'workSymbolCodes';
const VALUATION_CATEGORY_FEATURE_PROPERTY = 'valuationCategory';
const OPEN_WORK_SYMBOLS_FEATURE_PROPERTY = 'openWorkSymbolCodes';
const MANAGEMENT_FEATURE_PROPERTY = 'management';
const SPECIES_GROUP_FEATURE_PROPERTY = 'speciesGroup';
const MARKING_CODES_FEATURE_PROPERTY = 'markingCodes';

/** Rozmiary V1 (piksele): centralny symbol drzewa i mniejsze znaczki prac wokol niego. */
const TREE_SYMBOL_RADIUS_PX = 8;
const WORK_BADGE_RADIUS_PX = 6;
const NEUTRAL_TREE_STYLE = createTreeMarkerStyle(SAVED_TREE_MARKER_COLOR);
const NEUTRAL_CONIFER_STYLE = createTreeMarkerStyle(SAVED_TREE_MARKER_COLOR, 'star');
const shapeOf = (speciesGroup: SpeciesGroup | null): TreeMarkerShape => (speciesGroup === 'CONIFER' ? 'star' : 'circle');

/** Grupy drzew (etap 29): polprzezroczysty zielony obszar z nazwa - pod markerami drzew. */
const standStyleOf = (name: string) => new Style({
  fill: new Fill({ color: 'rgba(64, 160, 43, 0.18)' }),
  stroke: new Stroke({ color: '#2f9e44', width: 2 }),
  text: new Text({ text: name, font: '600 12px sans-serif', fill: new Fill({ color: '#1b4d2a' }),
    stroke: new Stroke({ color: '#ffffff', width: 3 }), overflow: true }),
});

/**
 * Style jednego drzewa: centralny marker (symbol drzewa albo neutralny) + znaczki symboli prac przesuniete
 * wizualnie wokol punktu (displacement - wspolrzedne drzewa sie nie zmieniaja) + "+N" przy nadmiarze.
 * Nieznany kod (spoza katalogu) = neutralny wyglad / pominiety znaczek zamiast bledu mapy.
 */
function buildTreeStyles(treeSymbolCode: string | null, workSymbolCodes: string[],
                         symbolByCode: globalThis.Map<string, MapSymbol>, centerStyle?: Style, speciesGroup: SpeciesGroup | null = null): Style[] {
  const treeSymbol = treeSymbolCode ? symbolByCode.get(treeSymbolCode) : undefined;
  const styles = centerStyle ? [centerStyle]
    : treeSymbol ? createMapSymbolStyles(treeSymbol, { radius: TREE_SYMBOL_RADIUS_PX })
      : [speciesGroup === 'CONIFER' ? NEUTRAL_CONIFER_STYLE : NEUTRAL_TREE_STYLE];
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
 * Oznaczenia rysujemy dopiero od przyblizenia "osiedle" (ok. zoom 15; m na piksel) - przy widoku miasta rzedy
 * oznaczen sasiednich drzew nachodza na siebie. Wartosc V1.
 */
const MARKING_BADGES_MAXIMUM_RESOLUTION = 5;

/** Tryb "Oznaczenia drzew" (etap 31): neutralny punkt drzewa + rzad oznaczen nad nim (symbole prac ukryte). */
function buildMarkingStyles(markingCodes: string[], markingByCode: globalThis.Map<string, MapMarkingAppearance>,
                            speciesGroup: SpeciesGroup | null): Style[] {
  return [speciesGroup === 'CONIFER' ? NEUTRAL_CONIFER_STYLE : NEUTRAL_TREE_STYLE, ...createTreeMarkingStyles(markingCodes, markingByCode)];
}

/** Tryb "Waloryzacja" (etap 27): sam kolor kategorii, bez znaczkow prac - nie mieszamy dwoch znaczen na jednym punkcie. */
function buildValuationStyles(category: TreeValuationCategory | null, speciesGroup: SpeciesGroup | null): Style[] {
  return [createTreeMarkerStyle(valuationColorOf(category).fill, shapeOf(speciesGroup))];
}

/**
 * Tryb "Gospodarka drzewostanem" (etap 28): kolor markera = stan prac drzewa (pilne / do wykonania / tylko zamkniete /
 * brak), znaczki tylko prac do wykonania - wykonane i anulowane nie zasmiecaja mapy.
 */
function buildManagementStyles(management: TreeManagementSummary, openWorkSymbolCodes: string[],
                               symbolByCode: globalThis.Map<string, MapSymbol>, speciesGroup: SpeciesGroup | null): Style[] {
  return buildTreeStyles(null, openWorkSymbolCodes, symbolByCode,
    createTreeMarkerStyle(managementColorOf(management.state), shapeOf(speciesGroup)));
}

/**
 * Mapa projektu (OpenLayers): podklad OSM + warstwa markerow drzew. Tryby (viewMode): symbole drzew i prac (etap 19)
 * albo kolor waloryzacji (etap 27) albo stan prac (etap 28) - zmiana trybu tylko przerysowuje style tej samej warstwy.
 * Tylko podglad - nie edytuje lokalizacji ani symboli.
 * Instancja mapy powstaje raz (przy montowaniu) i jest niszczona przy odmontowaniu; zmiana punktow podmienia
 * jedynie Feature w warstwie i dopasowuje widok. Klikniecie markera zglasza treeId (albo null - klik obok).
 */
export function ProjectTreeMap({ treeMapPoints, symbolByCode, onTreeMarkerClicked, viewMode = 'SYMBOLS', stands = [],
                                 markingByCode = NO_MARKINGS, focusedTreeId = null, onEmptyMapClicked,
                                 referenceLayers = NO_REFERENCE_LAYERS, fitViewOnPointsChange = 'always',
                                 livePosition = null, selectedTreeId = null, pendingLocation = null,
                                 fillsContainer = false, centerOn = null }: {
  /**
   * etap 32: 'always' (domyslnie) - kazda zmiana punktow dopasowuje widok do drzew; 'firstLoadOnly' (teren) - tylko
   * pierwsze punkty, zeby mapa nie skakala po zapisaniu drzewa
   */
  fitViewOnPointsChange?: 'always' | 'firstLoadOnly';
  /** etap 32: biezaca pozycja (osobna, nieklikalna warstwa nad drzewami) */
  livePosition?: LivePositionOnMap | null;
  /** etap 32: wyroznienie wybranego drzewa pierscieniem */
  selectedTreeId?: number | null;
  /** etap 32: tymczasowy punkt przed zapisem (podglad pomiaru GPS albo wskazania na mapie) */
  pendingLocation?: TreeLocation | null;
  /** etap 32: mapa na cala wysokosc kontenera zamiast stalej wysokosci .map-container */
  fillsContainer?: boolean;
  /**
   * etap 32: "Moja pozycja" - props z requestId zamiast refa z useImperativeHandle: prosciej (bez forwardRef),
   * a nowy requestId wystarcza, zeby wysrodkowac ponownie na tym samym punkcie
   */
  centerOn?: MapCenterRequest | null;
  /** etap 31: widoczne warstwy referencyjne projektu (podklad wektorowy z QGIS) - pod grupami drzew i drzewami */
  referenceLayers?: ReferenceLayerOnMap[];
  /** etap 31: tryb "Dodaj drzewo na mapie" - klik obok markerow zglasza punkt WGS 84 (brak = zwykly podglad) */
  onEmptyMapClicked?: (location: TreeLocation) => void;
  treeMapPoints: TreeMapPoint[];
  /** etap 31: katalog oznaczen (wyglad) - tryb "Oznaczenia drzew" */
  markingByCode?: globalThis.Map<string, MapMarkingAppearance>;
  /** etap 31: drzewo do pokazania (np. "Pokaz na mapie" z karty) - widok przybliza sie do niego zamiast do calosci */
  focusedTreeId?: number | null;
  /** etap 29: grupy drzew pokazywane jako obszary (pusta lista = warstwa ukryta) */
  stands?: StandMapShape[];
  symbolByCode: globalThis.Map<string, MapSymbol>;
  viewMode?: MapViewMode;
  onTreeMarkerClicked: (treeId: number | null) => void;
}) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const treeMarkerSourceRef = useRef<VectorSource<Feature<Point>> | null>(null);
  // Najnowszy callback w refie - listener OpenLayers rejestrujemy raz i nie chcemy starego domkniecia.
  const onTreeMarkerClickedRef = useRef(onTreeMarkerClicked);
  onTreeMarkerClickedRef.current = onTreeMarkerClicked;
  const onEmptyMapClickedRef = useRef(onEmptyMapClicked);
  onEmptyMapClickedRef.current = onEmptyMapClicked;
  // Katalog symboli w refie + pamiec stylow wg kombinacji symboli (wiele drzew ma te same symbole).
  const symbolByCodeRef = useRef(symbolByCode);
  const styleCacheRef = useRef(new globalThis.Map<string, Style[]>());
  const treeMarkerLayerRef = useRef<VectorLayer<VectorSource<Feature<Point>>> | null>(null);
  const viewModeRef = useRef(viewMode);
  const standSourceRef = useRef<VectorSource<Feature<Polygon>> | null>(null);
  const referenceSourceRef = useRef<VectorSource | null>(null);
  const markingByCodeRef = useRef(markingByCode);
  const focusedTreeIdRef = useRef(focusedTreeId);
  focusedTreeIdRef.current = focusedTreeId;
  const fitViewOnPointsChangeRef = useRef(fitViewOnPointsChange);
  fitViewOnPointsChangeRef.current = fitViewOnPointsChange;
  const hasFittedViewToPointsRef = useRef(false);
  const livePositionSourceRef = useRef<VectorSource | null>(null);
  const highlightSourceRef = useRef<VectorSource | null>(null);

  useEffect(() => {
    if (!mapContainerRef.current) return;
    const treeMarkerSource = new VectorSource<Feature<Point>>();
    const treeMarkerLayer = new VectorLayer({
      source: treeMarkerSource,
      style: (feature: FeatureLike, resolution: number) => {
        const speciesGroup = (feature.get(SPECIES_GROUP_FEATURE_PROPERTY) as SpeciesGroup | null) ?? null;
        if (viewModeRef.current === 'VALUATION') {
          const category = (feature.get(VALUATION_CATEGORY_FEATURE_PROPERTY) as TreeValuationCategory | null) ?? null;
          const valuationKey = `valuation|${category}|${speciesGroup}`;
          let valuationStyles = styleCacheRef.current.get(valuationKey);
          if (!valuationStyles) {
            valuationStyles = buildValuationStyles(category, speciesGroup);
            styleCacheRef.current.set(valuationKey, valuationStyles);
          }
          return valuationStyles;
        }
        if (viewModeRef.current === 'MARKINGS') {
          const markingCodes = resolution > MARKING_BADGES_MAXIMUM_RESOLUTION ? []
            : (feature.get(MARKING_CODES_FEATURE_PROPERTY) as string[] | undefined) ?? [];
          const markingKey = `markings|${markingCodes.join(',')}|${speciesGroup}`;
          let markingStyles = styleCacheRef.current.get(markingKey);
          if (!markingStyles) {
            markingStyles = buildMarkingStyles(markingCodes, markingByCodeRef.current, speciesGroup);
            styleCacheRef.current.set(markingKey, markingStyles);
          }
          return markingStyles;
        }
        if (viewModeRef.current === 'MANAGEMENT') {
          const management = feature.get(MANAGEMENT_FEATURE_PROPERTY) as TreeManagementSummary;
          const openWorkSymbolCodes = (feature.get(OPEN_WORK_SYMBOLS_FEATURE_PROPERTY) as string[] | undefined) ?? [];
          const managementKey = `management|${management.state}|${openWorkSymbolCodes.join(',')}|${speciesGroup}`;
          let managementStyles = styleCacheRef.current.get(managementKey);
          if (!managementStyles) {
            managementStyles = buildManagementStyles(management, openWorkSymbolCodes, symbolByCodeRef.current, speciesGroup);
            styleCacheRef.current.set(managementKey, managementStyles);
          }
          return managementStyles;
        }
        const treeSymbolCode = (feature.get(TREE_SYMBOL_FEATURE_PROPERTY) as string | null) ?? null;
        const workSymbolCodes = (feature.get(WORK_SYMBOLS_FEATURE_PROPERTY) as string[] | undefined) ?? [];
        const styleKey = `${treeSymbolCode}|${workSymbolCodes.join(',')}|${speciesGroup}`;
        let styles = styleCacheRef.current.get(styleKey);
        if (!styles) {
          styles = buildTreeStyles(treeSymbolCode, workSymbolCodes, symbolByCodeRef.current, undefined, speciesGroup);
          styleCacheRef.current.set(styleKey, styles);
        }
        return styles;
      },
    });
    const standSource = new VectorSource<Feature<Polygon>>();
    // declutter: nachodzace na siebie nazwy grup sa pomijane (obszary rysowane zawsze).
    const standLayer = new VectorLayer({ source: standSource, declutter: true,
      style: (feature: FeatureLike) => standStyleOf(String(feature.get('name') ?? '')) });
    standSourceRef.current = standSource;
    const referenceSource = new VectorSource();
    referenceSourceRef.current = referenceSource;
    const referenceLayer = new VectorLayer({ source: referenceSource,
      style: (feature: FeatureLike) => referenceStyleOf(String(feature.get(REFERENCE_COLOR_PROPERTY) ?? '#495057')) });
    // Etap 32: wyroznienie wybranego drzewa i punkt przed zapisem (nad drzewami) oraz biezaca pozycja (na wierzchu).
    // Obie warstwy nieklikalne - klik sprawdza tylko warstwe drzew (layerFilter).
    const highlightSource = new VectorSource();
    highlightSourceRef.current = highlightSource;
    const livePositionSource = new VectorSource();
    livePositionSourceRef.current = livePositionSource;
    const map = createMapWithBasemap(mapContainerRef.current, [referenceLayer, standLayer, treeMarkerLayer,
      new VectorLayer({ source: highlightSource }), new VectorLayer({ source: livePositionSource })]);
    treeMarkerLayerRef.current = treeMarkerLayer;

    map.on('singleclick', (clickEvent) => {
      const clickedFeature = map.forEachFeatureAtPixel(clickEvent.pixel, (feature) => feature,
        { hitTolerance: MARKER_HIT_TOLERANCE_PX, layerFilter: (layer) => layer === treeMarkerLayer });
      const clickedTreeId = clickedFeature?.get(TREE_ID_FEATURE_PROPERTY);
      if (typeof clickedTreeId !== 'number' && onEmptyMapClickedRef.current) {
        onEmptyMapClickedRef.current(toTreeLocation(clickEvent.coordinate));
        return;
      }
      onTreeMarkerClickedRef.current(typeof clickedTreeId === 'number' ? clickedTreeId : null);
    });
    // Kursor "reki" nad markerem (desktop) - sygnal, ze marker jest klikalny.
    map.on('pointermove', (pointerEvent) => {
      if (pointerEvent.dragging) return;
      const isOverTreeMarker = map.hasFeatureAtPixel(pointerEvent.pixel,
        { hitTolerance: MARKER_HIT_TOLERANCE_PX, layerFilter: (layer) => layer === treeMarkerLayer });
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

  // Zmiana trybu widoku - te same punkty, inne style (bez przebudowy warstwy i bez zmiany widoku mapy).
  useEffect(() => {
    viewModeRef.current = viewMode;
    treeMarkerLayerRef.current?.changed();
  }, [viewMode]);

  // Katalogi symboli i oznaczen dochodza asynchronicznie - po ich zmianie przerysowujemy markery nowymi stylami.
  useEffect(() => {
    symbolByCodeRef.current = symbolByCode;
    markingByCodeRef.current = markingByCode;
    styleCacheRef.current.clear();
    treeMarkerLayerRef.current?.changed();
  }, [symbolByCode, markingByCode]);

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
      [VALUATION_CATEGORY_FEATURE_PROPERTY]: treeMapPoint.valuationCategory,
      [OPEN_WORK_SYMBOLS_FEATURE_PROPERTY]: treeMapPoint.openWorkSymbolCodes,
      [MANAGEMENT_FEATURE_PROPERTY]: treeMapPoint.management,
      [SPECIES_GROUP_FEATURE_PROPERTY]: treeMapPoint.speciesGroup,
      [MARKING_CODES_FEATURE_PROPERTY]: treeMapPoint.markingCodes,
    })));
    if (fitViewOnPointsChangeRef.current === 'firstLoadOnly' && hasFittedViewToPointsRef.current) return;
    const focusedPoint = treeMapPoints.find((treeMapPoint) => treeMapPoint.treeId === focusedTreeIdRef.current);
    fitViewToCoordinates(map, focusedPoint ? [focusedPoint.mapCoordinate]
      : treeMapPoints.map((treeMapPoint) => treeMapPoint.mapCoordinate));
    // Projekt bez drzew (widok Polski) - pierwsze dodane drzewo jeszcze dopasuje widok.
    if (treeMapPoints.length > 0) hasFittedViewToPointsRef.current = true;
  }, [treeMapPoints]);

  // Etap 32: wybrane drzewo (pierscien) i punkt przed zapisem - zmiana nie zmienia widoku mapy.
  useEffect(() => {
    const highlightSource = highlightSourceRef.current;
    if (!highlightSource) return;
    highlightSource.clear();
    const selectedPoint = selectedTreeId === null ? undefined : treeMapPoints.find((point) => point.treeId === selectedTreeId);
    if (selectedPoint) {
      const ring = new Feature({ geometry: new Point(selectedPoint.mapCoordinate) });
      ring.setStyle(SELECTED_TREE_RING_STYLE);
      highlightSource.addFeature(ring);
    }
    if (pendingLocation) {
      if (pendingLocation.accuracyM) {
        const accuracyArea = new Feature({
          geometry: accuracyCircleOf(pendingLocation.latitude, pendingLocation.longitude, pendingLocation.accuracyM) });
        accuracyArea.setStyle(PENDING_ACCURACY_STYLE);
        highlightSource.addFeature(accuracyArea);
      }
      const pendingPoint = new Feature({ geometry: new Point(toMapCoordinate(pendingLocation)) });
      pendingPoint.setStyle(PENDING_LOCATION_STYLE);
      highlightSource.addFeature(pendingPoint);
    }
  }, [selectedTreeId, pendingLocation, treeMapPoints]);

  // Etap 32: biezaca pozycja uzytkownika.
  useEffect(() => {
    const livePositionSource = livePositionSourceRef.current;
    if (!livePositionSource) return;
    livePositionSource.clear();
    if (!livePosition) return;
    const accuracyArea = new Feature({ geometry: accuracyCircleOf(livePosition.latitude, livePosition.longitude, livePosition.accuracyM) });
    accuracyArea.setStyle(LIVE_POSITION_ACCURACY_STYLE);
    const dot = new Feature({ geometry: new Point(fromLonLat([livePosition.longitude, livePosition.latitude])) });
    dot.setStyle(LIVE_POSITION_DOT_STYLE);
    livePositionSource.addFeatures([accuracyArea, dot]);
  }, [livePosition]);

  // Etap 32: "Moja pozycja" - wysrodkowanie z przyblizeniem co najmniej do poziomu dzialki.
  useEffect(() => {
    const view = mapRef.current?.getView();
    if (!view || !centerOn) return;
    view.animate({ center: fromLonLat([centerOn.longitude, centerOn.latitude]),
      zoom: Math.max(view.getZoom() ?? 0, TREE_FOCUS_ZOOM - 1), duration: 300 });
  }, [centerOn]);

  // Warstwy referencyjne (etap 31) - jedno zrodlo dla wszystkich widocznych warstw; zmiana nie zmienia widoku mapy.
  useEffect(() => {
    const referenceSource = referenceSourceRef.current;
    if (!referenceSource) return;
    referenceSource.clear();
    const format = new GeoJSON();
    for (const layer of referenceLayers) {
      try {
        const features = format.readFeatures(layer.geoJson, { dataProjection: 'EPSG:4326', featureProjection: 'EPSG:3857' });
        features.forEach((feature) => feature.set(REFERENCE_COLOR_PROPERTY, layer.color));
        referenceSource.addFeatures(features);
      } catch {
        // Uszkodzona geometria jednej warstwy nie psuje mapy - warstwa po prostu sie nie pojawia.
      }
    }
  }, [referenceLayers]);

  // Grupy drzew (etap 29) - osobna warstwa pod markerami; zmiana listy nie zmienia widoku mapy.
  useEffect(() => {
    const standSource = standSourceRef.current;
    if (!standSource) return;
    standSource.clear();
    standSource.addFeatures(stands.map((stand) => new Feature({ geometry: standPolygonOf(stand), name: stand.name })));
  }, [stands]);

  return <div ref={mapContainerRef}
              className={`map-container${fillsContainer ? ' map-container-fill' : ''}${onEmptyMapClicked ? ' map-container-picking' : ''}`}
              role="region" aria-label="Mapa drzew projektu" />;
}

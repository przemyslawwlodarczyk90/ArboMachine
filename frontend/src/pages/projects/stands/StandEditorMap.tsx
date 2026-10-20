import { useEffect, useRef } from 'react';
import Feature from 'ol/Feature';
import type Map from 'ol/Map';
import Point from 'ol/geom/Point';
import Polygon from 'ol/geom/Polygon';
import Draw from 'ol/interaction/Draw';
import Modify from 'ol/interaction/Modify';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Fill, Stroke, Style, Text } from 'ol/style';
import { createMapWithBasemap, createTreeMarkerStyle, fitViewToCoordinates, SAVED_TREE_MARKER_COLOR } from '../../../map/openLayersBaseMap';
import type { TreeMapPoint } from '../../trees/map/treeMapPoints';
import type { StandPolygonGeometry } from './standDataTypes';
import { geometryFromMapRing, standPolygonOf, type StandMapShape } from './standGeometry';

const OTHER_STAND_STYLE = new Style({
  fill: new Fill({ color: 'rgba(120, 120, 120, 0.12)' }),
  stroke: new Stroke({ color: '#868e96', width: 1, lineDash: [4, 4] }),
});
const EDITED_STAND_STYLE = new Style({
  fill: new Fill({ color: 'rgba(64, 160, 43, 0.25)' }),
  stroke: new Stroke({ color: '#2f9e44', width: 3 }),
});
const TREE_STYLE = createTreeMarkerStyle(SAVED_TREE_MARKER_COLOR);

/**
 * Mapa edycji obszaru grupy drzew (etap 29): drzewa projektu (z numerami), pozostale grupy (szare) i edytowany obszar.
 * Bez obszaru - rysowanie (klik = wierzcholek, dwuklik albo klik w pierwszy punkt = koniec); z obszarem - przesuwanie
 * wierzcholkow. redrawToken - zmiana = "Rysuj od nowa". Kazda zmiana zglasza geometrie WGS 84 (albo null).
 */
export function StandEditorMap({ trees, otherStands, initialGeometry, redrawToken, onGeometryChange }: {
  trees: TreeMapPoint[];
  otherStands: StandMapShape[];
  initialGeometry: StandPolygonGeometry | null;
  redrawToken: number;
  onGeometryChange: (geometry: StandPolygonGeometry | null) => void;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const editSourceRef = useRef<VectorSource<Feature<Polygon>> | null>(null);
  const onGeometryChangeRef = useRef(onGeometryChange);
  onGeometryChangeRef.current = onGeometryChange;
  const initialGeometryRef = useRef(initialGeometry);

  useEffect(() => {
    if (!containerRef.current) return;
    const otherSource = new VectorSource<Feature<Polygon>>({
      features: otherStands.map((stand) => new Feature({ geometry: standPolygonOf(stand), name: stand.name })),
    });
    const treeSource = new VectorSource<Feature<Point>>({
      features: trees.map((tree) => new Feature({ geometry: new Point(tree.mapCoordinate), label: tree.inventoryNumber })),
    });
    const editSource = new VectorSource<Feature<Polygon>>();
    const map = createMapWithBasemap(containerRef.current, [
      new VectorLayer({ source: otherSource, style: OTHER_STAND_STYLE }),
      new VectorLayer({ source: editSource, style: EDITED_STAND_STYLE }),
      new VectorLayer({ source: treeSource, style: TREE_STYLE }),
      // Numery osobno: przy setkach drzew OpenLayers pomija kolidujace etykiety (declutter), punkty zostaja wszystkie.
      new VectorLayer({
        source: treeSource,
        declutter: true,
        style: (feature) => new Style({ text: new Text({ text: String(feature.get('label') ?? ''), offsetY: -16,
          font: '600 11px sans-serif', fill: new Fill({ color: '#1b4d2a' }), stroke: new Stroke({ color: '#fff', width: 3 }) }) }),
      }),
    ]);
    mapRef.current = map;
    editSourceRef.current = editSource;
    const coordinates = [...trees.map((tree) => tree.mapCoordinate),
      ...(initialGeometryRef.current ? standPolygonOf({ geometry: initialGeometryRef.current }).getCoordinates()[0] : [])];
    fitViewToCoordinates(map, coordinates);
    return () => {
      map.setTarget(undefined);
      map.dispose();
      mapRef.current = null;
      editSourceRef.current = null;
    };
    // Mapa powstaje raz; drzewa i inne grupy nie zmieniaja sie w trakcie edycji.
  }, []);

  // Tryb: rysowanie (brak obszaru) albo przesuwanie wierzcholkow (obszar jest). redrawToken > 0 = zaczynamy od nowa.
  useEffect(() => {
    const map = mapRef.current;
    const editSource = editSourceRef.current;
    if (!map || !editSource) return;
    editSource.clear();
    const startGeometry = redrawToken === 0 ? initialGeometryRef.current : null;
    const emit = () => {
      const feature = editSource.getFeatures()[0];
      onGeometryChangeRef.current(feature ? geometryFromMapRing(feature.getGeometry()!.getCoordinates()[0]) : null);
    };
    const modify = new Modify({ source: editSource });
    modify.on('modifyend', emit);
    let draw: Draw | null = null;
    if (startGeometry) {
      editSource.addFeature(new Feature({ geometry: standPolygonOf({ geometry: startGeometry }) }));
      map.addInteraction(modify);
    } else {
      draw = new Draw({ source: editSource, type: 'Polygon' });
      draw.on('drawend', () => {
        map.removeInteraction(draw!);
        map.addInteraction(modify);
        setTimeout(emit, 0);
      });
      map.addInteraction(draw);
      onGeometryChangeRef.current(null);
    }
    return () => {
      if (draw) map.removeInteraction(draw);
      map.removeInteraction(modify);
    };
  }, [redrawToken]);

  return <div ref={containerRef} className="map-container stand-editor-map" role="region" aria-label="Mapa obszaru grupy drzew" />;
}

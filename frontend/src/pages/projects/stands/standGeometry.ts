import type { Coordinate } from 'ol/coordinate.js';
import Polygon from 'ol/geom/Polygon.js';
import { fromLonLat, toLonLat } from 'ol/proj.js';
import type { StandPolygonGeometry, TreeStand } from './standDataTypes.ts';

// Geometria grup drzew (etap 29): GeoJSON WGS 84 <-> wspolrzedne mapy (EPSG:3857). Modul bez Reacta - node --test.

export type StandMapShape = Pick<TreeStand, 'id' | 'name' | 'geometry'>;

const round7 = (value: number) => Math.round(value * 1e7) / 1e7;

/** Obszar grupy jako wielokat OpenLayers we wspolrzednych mapy. */
export function standPolygonOf(stand: Pick<TreeStand, 'geometry'>): Polygon {
  return new Polygon(stand.geometry.coordinates.map((ring) => ring.map((position) => fromLonLat(position))));
}

/** Narysowany obrys (wspolrzedne mapy) -> GeoJSON Polygon WGS 84, zamkniety, 7 miejsc po przecinku. */
export function geometryFromMapRing(mapRing: Coordinate[]): StandPolygonGeometry | null {
  const ring = mapRing.map((coordinate) => toLonLat(coordinate)).map(([longitude, latitude]) => [round7(longitude), round7(latitude)]);
  const distinct = new Set(ring.map((position) => position.join(';')));
  if (distinct.size < 3) return null;
  const [first] = ring;
  const last = ring[ring.length - 1];
  if (first[0] !== last[0] || first[1] !== last[1]) ring.push([...first]);
  return { type: 'Polygon', coordinates: [ring] };
}

/** "850 m²" albo "1,25 ha" (od 1 ha) - przyblizone pole z backendu. */
export function formatStandArea(areaSquareMeters: number | null | undefined): string {
  if (areaSquareMeters === null || areaSquareMeters === undefined) return '—';
  if (areaSquareMeters >= 10_000) return `${(areaSquareMeters / 10_000).toLocaleString('pl-PL', { maximumFractionDigits: 2 })} ha`;
  return `${Math.round(areaSquareMeters).toLocaleString('pl-PL')} m²`;
}

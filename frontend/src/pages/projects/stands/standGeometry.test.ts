import assert from 'node:assert/strict';
import { test } from 'node:test';
import { fromLonLat } from 'ol/proj.js';
import { formatStandArea, geometryFromMapRing, standPolygonOf } from './standGeometry.ts';

test('narysowany obrys -> GeoJSON WGS 84 zamkniety, 7 miejsc; za malo punktów = brak', () => {
  const ring = [[21.149, 50.408], [21.15, 50.408], [21.15, 50.409]].map((position) => fromLonLat(position));
  const geometry = geometryFromMapRing(ring);
  assert.equal(geometry?.type, 'Polygon');
  assert.deepEqual(geometry?.coordinates[0], [[21.149, 50.408], [21.15, 50.408], [21.15, 50.409], [21.149, 50.408]]);
  assert.equal(geometryFromMapRing([fromLonLat([21, 50]), fromLonLat([21, 50]), fromLonLat([21.1, 50])]), null);
});

test('GeoJSON -> wielokąt mapy (EPSG:3857) i powrót', () => {
  const polygon = standPolygonOf({ geometry: { type: 'Polygon', coordinates: [[[21.149, 50.408], [21.15, 50.408], [21.15, 50.409], [21.149, 50.408]]] } });
  assert.deepEqual(geometryFromMapRing(polygon.getCoordinates()[0])?.coordinates[0][1], [21.15, 50.408]);
});

test('pole po polsku: m² do 1 ha, potem hektary', () => {
  assert.equal(formatStandArea(850.4), '850 m²');
  assert.equal(formatStandArea(12_500), '1,25 ha');
  assert.equal(formatStandArea(null), '—');
});

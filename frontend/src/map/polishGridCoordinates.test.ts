import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  formatGridMeters, fromPolishGrid, PL_1992_ZONE, pl2000ZoneByNumber, pl2000ZoneForLongitude, toPolishGrid,
} from './polishGridCoordinates.ts';

// Wartosci wzorcowe z PROJ (pyproj 3, 2026-10-20): [szerokosc, dlugosc, EPSG, easting (Y), northing (X)].
const REFERENCE_POINTS: [number, number, number, number, number][] = [
  [52.2297, 21.0122, 2178, 7500833.512, 5788456.487],
  [51.1079, 17.0385, 2176, 5642748.471, 5665630.246],
  [50.0647, 19.945, 2177, 6639245.607, 5549428.953],
  [53.1325, 23.1688, 2179, 8444371.356, 5889235.316],
  [52.2297, 21.0122, 2180, 637382.204, 486757.209],
  [54.35, 18.65, 2180, 477257.412, 720712.953],
];

const zoneOf = (epsgCode: number) => (epsgCode === 2180 ? PL_1992_ZONE : pl2000ZoneByNumber(epsgCode - 2171)!);

test('WGS 84 -> PL-2000 / PL-1992 zgodne z PROJ do 1 mm', () => {
  for (const [latitude, longitude, epsgCode, easting, northing] of REFERENCE_POINTS) {
    const result = toPolishGrid(latitude, longitude, zoneOf(epsgCode));
    assert.ok(Math.abs(result.y - easting) < 0.001, `EPSG:${epsgCode} Y ${result.y} != ${easting}`);
    assert.ok(Math.abs(result.x - northing) < 0.001, `EPSG:${epsgCode} X ${result.x} != ${northing}`);
  }
});

test('PL-2000 / PL-1992 -> WGS 84 wraca do punktu wyjscia (ok. 1 cm = 1e-7 stopnia)', () => {
  for (const [latitude, longitude, epsgCode, easting, northing] of REFERENCE_POINTS) {
    const result = fromPolishGrid({ x: northing, y: easting }, zoneOf(epsgCode));
    assert.ok(Math.abs(result.latitude - latitude) < 1e-7);
    assert.ok(Math.abs(result.longitude - longitude) < 1e-7);
  }
});

test('strefa PL-2000 z dlugosci geograficznej; poza Polska najblizsza strefa', () => {
  assert.equal(pl2000ZoneForLongitude(14.2).zoneNumber, 5);
  assert.equal(pl2000ZoneForLongitude(16.49).zoneNumber, 5);
  assert.equal(pl2000ZoneForLongitude(16.51).zoneNumber, 6);
  assert.equal(pl2000ZoneForLongitude(21.0).zoneNumber, 7);
  assert.equal(pl2000ZoneForLongitude(24.1).zoneNumber, 8);
  assert.equal(pl2000ZoneForLongitude(2).zoneNumber, 5);
  assert.equal(pl2000ZoneForLongitude(30).zoneNumber, 8);
  assert.equal(pl2000ZoneByNumber(4), null);
});

test('format metrow po polsku: twarde spacje tysiecy, przecinek, centymetry', () => {
  assert.equal(formatGridMeters(5788456.487), '5 788 456,49');
  assert.equal(formatGridMeters(486757.2), '486 757,20');
});

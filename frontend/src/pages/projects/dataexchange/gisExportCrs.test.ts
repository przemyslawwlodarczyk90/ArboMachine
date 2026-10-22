import assert from 'node:assert/strict';
import { test } from 'node:test';
import { DEFAULT_GIS_EXPORT_CRS, exportQueryOf, GIS_EXPORT_CRS_OPTIONS } from './gisExportCrs.ts';

// Uruchamianie: npm test (node --test). Uklad wspolrzednych eksportu do QGIS (etap 34).

test('uklad dodawany tylko do GeoPackage i pakietu QGIS', () => {
  assert.equal(exportQueryOf('gpkg', 'EPSG:2178'), '?crs=EPSG%3A2178');
  assert.equal(exportQueryOf('qgis-zip', 'PL-2000'), '?crs=PL-2000');
  assert.equal(exportQueryOf('geojson', 'EPSG:2178'), '');
  assert.equal(exportQueryOf('csv', 'PL-2000'), '');
  assert.equal(exportQueryOf('gpkg', null), '');
});

test('lista ukladow: domyslny PL-2000, wszystkie strefy, PL-1992 i WGS 84, bez powtorzen', () => {
  const codes = GIS_EXPORT_CRS_OPTIONS.map((option) => option.crs);
  assert.equal(DEFAULT_GIS_EXPORT_CRS, 'PL-2000');
  assert.equal(codes[0], DEFAULT_GIS_EXPORT_CRS);
  assert.deepEqual([...codes].sort(), ['EPSG:2176', 'EPSG:2177', 'EPSG:2178', 'EPSG:2179', 'EPSG:2180', 'EPSG:4326', 'PL-2000']);
});

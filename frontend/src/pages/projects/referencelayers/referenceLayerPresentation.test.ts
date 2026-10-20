import assert from 'node:assert/strict';
import { test } from 'node:test';
import { describeRejectedLayerFile, REFERENCE_LAYER_COLORS, referenceLayerColorOf } from './referenceLayerPresentation.ts';

test('kolory warstw powtarzaja sie po wyczerpaniu palety', () => {
  assert.equal(referenceLayerColorOf(0), REFERENCE_LAYER_COLORS[0]);
  assert.equal(referenceLayerColorOf(REFERENCE_LAYER_COLORS.length + 1), REFERENCE_LAYER_COLORS[1]);
});

test('komunikaty odrzuconego pliku po polsku, z nazwa ukladu, gdy jest', () => {
  assert.match(describeRejectedLayerFile('UNSUPPORTED_CRS', 'EPSG:3120'), /\(EPSG:3120\)/);
  assert.doesNotMatch(describeRejectedLayerFile('UNSUPPORTED_CRS', '?'), /\(\?\)/);
  assert.match(describeRejectedLayerFile('INVALID_COORDINATES'), /poza zakresem/);
  assert.equal(describeRejectedLayerFile(undefined), 'Nie udało się dodać warstwy.');
});

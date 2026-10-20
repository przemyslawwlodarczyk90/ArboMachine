import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  toTreeLocationFormValues, toTreeLocationRequest, validateTreeLocationFormValues, type TreeLocationFormValues,
} from './treeLocationFormValues.ts';

const formValues = (change: Partial<TreeLocationFormValues>): TreeLocationFormValues =>
  ({ ...toTreeLocationFormValues(null), ...change });

test('WGS 84: przecinek dziesietny, zrodlo MANUAL', () => {
  const values = formValues({ latitude: '52,2297', longitude: '21.0122' });
  assert.deepEqual(validateTreeLocationFormValues(values), {});
  assert.deepEqual(toTreeLocationRequest(values), { latitude: 52.2297, longitude: 21.0122, source: 'MANUAL' });
});

test('PL-2000 strefa 7 ze spacjami tysiecy -> WGS 84 (wartosc wzorcowa PROJ)', () => {
  const values = formValues({ coordinateSystem: 'PL_2000_7', gridX: '5 788 456,487', gridY: '7500833.512' });
  assert.deepEqual(validateTreeLocationFormValues(values), {});
  assert.deepEqual(toTreeLocationRequest(values), { latitude: 52.2297, longitude: 21.0122, source: 'MANUAL' });
});

test('PL-1992 -> WGS 84', () => {
  const request = toTreeLocationRequest(formValues({ coordinateSystem: 'PL_1992', gridX: '486757.209', gridY: '637382.204' }));
  assert.equal(request.latitude, 52.2297);
  assert.equal(request.longitude, 21.0122);
});

test('uklad krajowy: brak / tekst w polach i punkt poza Polska (zamienione X i Y) to bledy', () => {
  assert.deepEqual(Object.keys(validateTreeLocationFormValues(formValues({ coordinateSystem: 'PL_2000_7', gridY: 'abc' }))).sort(),
    ['gridX', 'gridY']);
  const swapped = validateTreeLocationFormValues(formValues({ coordinateSystem: 'PL_2000_7', gridX: '7500833.512', gridY: '5788456.487' }));
  assert.match(swapped.gridX, /poza Polską/);
});

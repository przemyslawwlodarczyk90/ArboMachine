import assert from 'node:assert/strict';
import { test } from 'node:test';
import { fromLonLat } from 'ol/proj.js';
import type { Tree } from '../treeDataTypes';
import { toTreeMapPoints } from './treeMapPoints.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci).

const treeWith = (id: number, location: Tree['location']): Tree => ({
  id,
  projectId: 1,
  inventoryNumber: `A-${id}`,
  location,
  createdAt: '2026-09-30T10:00:00Z',
  updatedAt: '2026-09-30T10:00:00Z',
});

test('pomija drzewa bez lokalizacji, zachowujac kolejnosc pozostalych', () => {
  const treeMapPoints = toTreeMapPoints([
    treeWith(1, { latitude: 52.1, longitude: 21.1 }),
    treeWith(2, null),
    treeWith(3, undefined),
    treeWith(4, { latitude: 50.06, longitude: 19.94 }),
  ]);
  assert.deepEqual(treeMapPoints.map((treeMapPoint) => treeMapPoint.treeId), [1, 4]);
});

test('punkt niesie treeId, inventoryNumber i wspolrzedna mapy [longitude, latitude]', () => {
  const [treeMapPoint] = toTreeMapPoints([treeWith(7, { latitude: 52.2296756, longitude: 21.0122287 })]);
  assert.equal(treeMapPoint.treeId, 7);
  assert.equal(treeMapPoint.inventoryNumber, 'A-7');
  assert.deepEqual(treeMapPoint.mapCoordinate, fromLonLat([21.0122287, 52.2296756]));
});

test('pomija punkt spoza zakresu WGS84 zamiast wysylac go na mape', () => {
  assert.deepEqual(toTreeMapPoints([treeWith(1, { latitude: 123, longitude: 21 })]), []);
});

test('pusty projekt daje pusta liste punktow', () => {
  assert.deepEqual(toTreeMapPoints([]), []);
});

test('laczy drzewa z pracami po treeId: symbol drzewa i symbole prac (tylko prace z symbolem)', () => {
  const [pointOfTree1, pointOfTree2] = toTreeMapPoints(
    [{ ...treeWith(1, { latitude: 52.1, longitude: 21.1 }), mapSymbolCode: 'GREEN_CIRCLE' }, treeWith(2, { latitude: 52.2, longitude: 21.2 })],
    [{ treeId: 1, mapSymbolCode: 'RED_CROSS' }, { treeId: 1, mapSymbolCode: null }, { treeId: 2, mapSymbolCode: 'BLUE_SQUARE' },
      { treeId: 1, mapSymbolCode: 'ORANGE_TRIANGLE' }, { treeId: 99, mapSymbolCode: 'BLACK_X' }]);
  assert.equal(pointOfTree1.treeSymbolCode, 'GREEN_CIRCLE');
  assert.deepEqual(pointOfTree1.workSymbolCodes, ['RED_CROSS', 'ORANGE_TRIANGLE']);
  assert.equal(pointOfTree2.treeSymbolCode, null);
  assert.deepEqual(pointOfTree2.workSymbolCodes, ['BLUE_SQUARE']);
});

test('drzewo bez symbolu i bez prac -> neutralny punkt bez znaczkow', () => {
  const [treeMapPoint] = toTreeMapPoints([treeWith(3, { latitude: 50, longitude: 20 })]);
  assert.equal(treeMapPoint.treeSymbolCode, null);
  assert.deepEqual(treeMapPoint.workSymbolCodes, []);
});

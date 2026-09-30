import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { MapSymbol } from './mapSymbolDataTypes';
import { buildProjectMapLegend } from './mapSymbolLegend.ts';
import { layoutWorkBadges, MAXIMUM_VISIBLE_WORK_BADGES, toMapSymbolShapeSpec } from './mapSymbolShapeSpec.ts';

// Uruchamianie: npm test (node --test). Czysta logika symboli mapowych - bez OpenLayers i bez DOM.

const symbol = (code: string, shape: MapSymbol['shape'], displayName: string): MapSymbol =>
  ({ code, displayName, shape, fillColor: '#000000', strokeColor: '#FFFFFF' });
const CATALOG = new Map<string, MapSymbol>([
  ['GREEN_CIRCLE', symbol('GREEN_CIRCLE', 'CIRCLE', 'Zielone koło')],
  ['RED_CROSS', symbol('RED_CROSS', 'CROSS', 'Czerwony krzyż')],
  ['ORANGE_TRIANGLE', symbol('ORANGE_TRIANGLE', 'TRIANGLE', 'Pomarańczowy trójkąt')],
]);

test('kazdy ksztalt katalogu ma swoja geometrie', () => {
  assert.deepEqual(toMapSymbolShapeSpec('CIRCLE'), { kind: 'circle' });
  assert.deepEqual(toMapSymbolShapeSpec('SQUARE'), { kind: 'polygon', points: 4, angle: Math.PI / 4, isStrokeOnly: false });
  assert.deepEqual(toMapSymbolShapeSpec('TRIANGLE'), { kind: 'polygon', points: 3, angle: 0, isStrokeOnly: false });
  assert.deepEqual(toMapSymbolShapeSpec('DIAMOND'), { kind: 'polygon', points: 4, angle: 0, isStrokeOnly: false });
  assert.deepEqual(toMapSymbolShapeSpec('CROSS'), { kind: 'polygon', points: 4, angle: 0, radius2: 0, isStrokeOnly: true });
  assert.deepEqual(toMapSymbolShapeSpec('X'), { kind: 'polygon', points: 4, angle: Math.PI / 4, radius2: 0, isStrokeOnly: true });
});

test('nieznany ksztalt -> bezpieczne kolo zamiast bledu', () => {
  assert.deepEqual(toMapSymbolShapeSpec('HEXAGON_FROM_NEWER_BACKEND'), { kind: 'circle' });
});

test('znaczki prac: 1, 2 i 3 prace w stalych miejscach wokol drzewa, bez nadmiaru', () => {
  assert.deepEqual(layoutWorkBadges([]), { badges: [], overflowCount: 0, overflowDisplacement: [-13, -13] });
  assert.deepEqual(layoutWorkBadges(['A']).badges, [{ symbolCode: 'A', displacement: [13, 13] }]);
  assert.deepEqual(layoutWorkBadges(['A', 'B']).badges.map((badge) => badge.displacement), [[13, 13], [-13, 13]]);
  assert.deepEqual(layoutWorkBadges(['A', 'B', 'C']).badges.map((badge) => badge.displacement), [[13, 13], [-13, 13], [13, -13]]);
});

test('znaczki prac: ponad limit -> pierwsze 3 + "+N"', () => {
  const layout = layoutWorkBadges(['A', 'B', 'C', 'D', 'E']);
  assert.equal(layout.badges.length, MAXIMUM_VISIBLE_WORK_BADGES);
  assert.equal(layout.overflowCount, 2);
});

test('legenda: symbole drzew i kombinacje "nazwa pracy + symbol" bez duplikatow', () => {
  const legend = buildProjectMapLegend(
    [{ id: 1, hasLocation: true, mapSymbolCode: 'GREEN_CIRCLE' }, { id: 2, hasLocation: true, mapSymbolCode: 'GREEN_CIRCLE' }],
    [
      { treeId: 1, name: 'Cięcia redukcyjne', mapSymbolCode: 'RED_CROSS' },
      { treeId: 2, name: ' cięcia redukcyjne ', mapSymbolCode: 'RED_CROSS' },
      { treeId: 2, name: 'Cięcia redukcyjne', mapSymbolCode: 'ORANGE_TRIANGLE' },
      { treeId: 1, name: 'Kontrola', mapSymbolCode: null },
    ],
    CATALOG);
  assert.deepEqual(legend.map((entry) => `${entry.kind}:${entry.symbol.code}:${entry.label}`), [
    'tree:GREEN_CIRCLE:Symbol drzewa — Zielone koło',
    'work:RED_CROSS:Cięcia redukcyjne',
    'work:ORANGE_TRIANGLE:Cięcia redukcyjne',
  ]);
});

test('legenda: tylko widoczne drzewa (z lokalizacja) i znane kody', () => {
  const legend = buildProjectMapLegend(
    [{ id: 1, hasLocation: false, mapSymbolCode: 'GREEN_CIRCLE' }, { id: 2, hasLocation: true, mapSymbolCode: 'PINK_STAR' }],
    [{ treeId: 1, name: 'Wycinka', mapSymbolCode: 'RED_CROSS' }, { treeId: 2, name: 'Wiązanie', mapSymbolCode: 'UNKNOWN' }],
    CATALOG);
  assert.deepEqual(legend, []);
});

test('legenda: tekst pracy pochodzi z nazwy pracy, nie z koloru symbolu', () => {
  const [entry] = buildProjectMapLegend([{ id: 1, hasLocation: true }],
    [{ treeId: 1, name: 'Kontrola za 12 miesięcy', mapSymbolCode: 'RED_CROSS' }], CATALOG);
  assert.equal(entry.label, 'Kontrola za 12 miesięcy');
});

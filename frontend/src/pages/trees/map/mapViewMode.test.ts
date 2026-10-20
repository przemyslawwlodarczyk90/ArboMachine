import assert from 'node:assert/strict';
import { test } from 'node:test';
import { buildMarkingLegend, buildValuationLegend, MAP_VIEW_MODES } from './mapViewMode.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci).

test('legenda waloryzacji: wszystkie kategorie z liczba drzew, takze zera i "nie oceniono"', () => {
  const legend = buildValuationLegend([{ valuationCategory: 'HIGH' }, { valuationCategory: 'HIGH' }, { valuationCategory: null }]);
  assert.deepEqual(legend, [
    { category: 'VERY_HIGH', count: 0 }, { category: 'HIGH', count: 2 }, { category: 'MEDIUM', count: 0 },
    { category: 'LOW', count: 0 }, { category: null, count: 1 },
  ]);
});

test('tryby mapy po polsku', () => {
  assert.deepEqual(MAP_VIEW_MODES.map((viewMode) => viewMode.label), ['Drzewa i symbole prac', 'Oznaczenia drzew', 'Waloryzacja', 'Gospodarka drzewostanem']);
});

test('legenda oznaczen: kolejnosc katalogu, liczba drzew, bez oznaczen nieuzytych', () => {
  const legend = buildMarkingLegend([{ markingCodes: ['SNAG', 'VALUABLE'] }, { markingCodes: ['SNAG'] }, { markingCodes: [] }],
    ['VALUABLE', 'TO_REMOVE', 'SNAG']);
  assert.deepEqual(legend, [{ code: 'VALUABLE', count: 1 }, { code: 'SNAG', count: 2 }]);
});

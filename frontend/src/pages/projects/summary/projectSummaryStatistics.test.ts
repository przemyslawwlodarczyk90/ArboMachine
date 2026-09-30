import assert from 'node:assert/strict';
import { test } from 'node:test';
import { toProjectSummaryStatistics } from './projectSummaryStatistics.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci).

test('kafelki podsumowania w stalej kolejnosci z polskimi etykietami', () => {
  const statistics = toProjectSummaryStatistics({
    treeCount: 3, treeWithLocationCount: 2, treeWithoutLocationCount: 1, photoCount: 3, workCount: 3,
  });
  assert.deepEqual(statistics.map((statistic) => `${statistic.label}: ${statistic.value}`), [
    'Drzewa: 3', 'Z lokalizacją: 2', 'Bez lokalizacji: 1', 'Zdjęcia: 3', 'Zalecenia i prace: 3',
  ]);
});

test('pusty projekt daje same zera, a nie pusta liste', () => {
  const statistics = toProjectSummaryStatistics({
    treeCount: 0, treeWithLocationCount: 0, treeWithoutLocationCount: 0, photoCount: 0, workCount: 0,
  });
  assert.equal(statistics.length, 5);
  assert.ok(statistics.every((statistic) => statistic.value === 0));
});

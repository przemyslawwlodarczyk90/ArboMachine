import assert from 'node:assert/strict';
import { test } from 'node:test';
import { filterTreesByValuation, valuationCategoryByTreeId } from './valuationFilter.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci).

const trees = [{ id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }];
const categories = valuationCategoryByTreeId([
  { treeId: 1, hasValuation: true, overallCategory: 'HIGH' },
  { treeId: 2, hasValuation: true, overallCategory: null, naturalValue: 'opis bez oceny ogólnej' },
  { treeId: 3, hasValuation: true, overallCategory: 'LOW' },
]);

test('filtr "wszystkie" i po kategorii', () => {
  assert.deepEqual(filterTreesByValuation(trees, categories, 'ALL').map((tree) => tree.id), [1, 2, 3, 4]);
  assert.deepEqual(filterTreesByValuation(trees, categories, 'HIGH').map((tree) => tree.id), [1]);
  assert.deepEqual(filterTreesByValuation(trees, categories, 'VERY_HIGH'), []);
});

test('"nieocenione" = bez waloryzacji albo bez oceny ogólnej', () => {
  assert.deepEqual(filterTreesByValuation(trees, categories, 'NOT_ASSESSED').map((tree) => tree.id), [2, 4]);
});

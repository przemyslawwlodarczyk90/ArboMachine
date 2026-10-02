import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  areTreeWorkFormValuesEquivalent, emptyTreeWorkFormValues, toCompletedTreeWorkRequest, toTreeWorkFieldsRequest, validateTreeWorkFormValues,
  type TreeWorkFormValues,
} from './treeWorkFormValues.ts';
import type { TreeWork } from './treeWorkDataTypes.ts';

const values = (overrides: Partial<TreeWorkFormValues>): TreeWorkFormValues => ({ ...emptyTreeWorkFormValues, ...overrides });

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci).

test('nazwa jest wymagana (same spacje to brak nazwy)', () => {
  assert.equal(validateTreeWorkFormValues(values({ name: '   ', description: '' })).name, 'Nazwa jest wymagana.');
  assert.deepEqual(validateTreeWorkFormValues(values({ name: 'Cięcia redukcyjne', description: '' })), {});
});

test('limity dlugosci jak w backendzie: nazwa 255, opis 5000', () => {
  assert.deepEqual(validateTreeWorkFormValues(values({ name: 'a'.repeat(255), description: 'b'.repeat(5000) })), {});
  const errors = validateTreeWorkFormValues(values({ name: 'a'.repeat(256), description: 'b'.repeat(5001) }));
  assert.match(errors.name, /255/);
  assert.match(errors.description, /5000/);
});

test('request: nazwa przycieta, pusty opis pominiety', () => {
  assert.deepEqual(toTreeWorkFieldsRequest(values({ name: '  Wycinka  ', description: '   ' })), { name: 'Wycinka', description: undefined, status: 'RECOMMENDED', priority: undefined, plannedDate: undefined, completedDate: undefined });
  assert.deepEqual(toTreeWorkFieldsRequest(values({ name: 'Wycinka', description: ' Pilne ' })), { name: 'Wycinka', description: 'Pilne', status: 'RECOMMENDED', priority: undefined, plannedDate: undefined, completedDate: undefined });
});

test('dopisanie samych spacji nie jest zmiana formularza', () => {
  assert.ok(areTreeWorkFormValuesEquivalent(values({ name: 'Wycinka', description: '' }), values({ name: ' Wycinka ', description: '  ' })));
  assert.ok(!areTreeWorkFormValuesEquivalent(values({ name: 'Wycinka', description: '' }), values({ name: 'Wycinka', description: 'x' })));
});

test('etap 28: status, priorytet i daty w request; puste pola pominiete; data w przeszlosci dozwolona', () => {
  assert.deepEqual(toTreeWorkFieldsRequest(values({ name: 'Cięcia', status: 'PLANNED', priority: 'URGENT', plannedDate: '2019-05-01' })), {
    name: 'Cięcia', description: undefined, status: 'PLANNED', priority: 'URGENT', plannedDate: '2019-05-01', completedDate: undefined,
  });
  assert.deepEqual(validateTreeWorkFormValues(values({ name: 'Cięcia', plannedDate: '2019-05-01', completedDate: '2018-01-01' })), {});
  assert.equal(validateTreeWorkFormValues(values({ name: 'Cięcia', plannedDate: '15.11.2026' })).plannedDate, 'Podaj poprawną datę.');
  assert.ok(!areTreeWorkFormValuesEquivalent(values({ name: 'A' }), values({ name: 'A', status: 'COMPLETED' })));
});

test('oznaczenie jako wykonana: komplet pol, status COMPLETED, data dzisiejsza tylko gdy brak', () => {
  const treeWork: TreeWork = { id: 5, treeId: 1, name: 'Kontrola', description: 'opis', status: 'PLANNED', priority: 'HIGH',
    plannedDate: '2026-10-10', completedDate: null, createdAt: '', updatedAt: '' };
  assert.deepEqual(toCompletedTreeWorkRequest(treeWork, '2026-10-01'), {
    name: 'Kontrola', description: 'opis', status: 'COMPLETED', priority: 'HIGH', plannedDate: '2026-10-10', completedDate: '2026-10-01',
  });
  assert.equal(toCompletedTreeWorkRequest({ ...treeWork, completedDate: '2026-09-01' }, '2026-10-01').completedDate, '2026-09-01');
});

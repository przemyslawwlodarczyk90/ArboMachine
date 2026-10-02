import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  areMonitoringFormValuesEquivalent, emptyMonitoringFormValues, filterTreesByMonitoring, latestRecordByTreeId,
  toMonitoringFormValues, toMonitoringRequest, validateMonitoringFormValues,
} from './monitoringFormValues.ts';
import { healthStatusLabel } from './monitoringDataTypes.ts';

// Uruchamianie: npm test (node --test).

test('nowa kontrola: dzisiejsza data, bez oceny; request pomija puste pola i przycina uwagi', () => {
  const values = emptyMonitoringFormValues('2026-10-02');
  assert.deepEqual(toMonitoringRequest(values), { inspectionDate: '2026-10-02', healthStatus: undefined, notes: undefined });
  assert.deepEqual(toMonitoringRequest({ ...values, healthStatus: 'POOR', notes: '  Pęknięcie konaru ' }),
    { inspectionDate: '2026-10-02', healthStatus: 'POOR', notes: 'Pęknięcie konaru' });
});

test('walidacja: data wymagana i poprawna, uwagi do 5000 znaków; data historyczna dozwolona', () => {
  assert.equal(validateMonitoringFormValues({ inspectionDate: '', healthStatus: '', notes: '' }).inspectionDate, 'Podaj datę kontroli.');
  assert.equal(validateMonitoringFormValues({ inspectionDate: '02.10.2026', healthStatus: '', notes: '' }).inspectionDate, 'Podaj poprawną datę.');
  assert.match(validateMonitoringFormValues({ inspectionDate: '2026-10-02', healthStatus: '', notes: 'x'.repeat(5001) }).notes ?? '', /5000/);
  assert.deepEqual(validateMonitoringFormValues({ inspectionDate: '2015-04-01', healthStatus: 'GOOD', notes: '' }), {});
});

test('edycja: wartości z zapisanego wpisu; dopisanie spacji to nie zmiana', () => {
  const values = toMonitoringFormValues({ id: 1, treeId: 2, inspectionDate: '2026-09-20', healthStatus: null, notes: null,
    createdAt: '', updatedAt: '' });
  assert.deepEqual(values, { inspectionDate: '2026-09-20', healthStatus: '', notes: '' });
  assert.ok(areMonitoringFormValuesEquivalent(values, { ...values, notes: '   ' }));
  assert.ok(!areMonitoringFormValuesEquivalent(values, { ...values, healthStatus: 'FAIR' }));
});

test('etykiety po polsku; brak oceny = Nie określono', () => {
  assert.equal(healthStatusLabel('GOOD'), 'Dobry');
  assert.equal(healthStatusLabel('FAIR'), 'Umiarkowany');
  assert.equal(healthStatusLabel('POOR'), 'Zły');
  assert.equal(healthStatusLabel(null), 'Nie określono');
});

test('filtr listy drzew po ostatniej kontroli', () => {
  const trees = [{ id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }];
  const latest = latestRecordByTreeId([
    { treeId: 1, inspectionDate: '2026-09-01', healthStatus: 'POOR' },
    { treeId: 2, inspectionDate: '2026-09-01', healthStatus: 'GOOD' },
    { treeId: 3, inspectionDate: '2026-09-01', healthStatus: null },
  ]);
  const ids = (filter: Parameters<typeof filterTreesByMonitoring>[2]) => filterTreesByMonitoring(trees, latest, filter).map((tree) => tree.id);
  assert.deepEqual(ids('ALL'), [1, 2, 3, 4]);
  assert.deepEqual(ids('NOT_INSPECTED'), [4]);
  assert.deepEqual(ids('POOR'), [1]);
  assert.deepEqual(ids('NOT_SPECIFIED'), [3]);
});

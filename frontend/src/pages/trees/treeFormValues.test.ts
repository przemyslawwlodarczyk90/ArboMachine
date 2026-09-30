import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { Tree } from './treeDataTypes';
import {
  areTreeFormValuesEquivalent, emptyTreeFormValues, toTreeEditableFieldsRequest, toTreeFormValues, validateTreeFormValues,
} from './treeFormValues.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci). Logika formularza terenowego drzewa.

const savedTree: Tree = {
  id: 1, projectId: 1, inventoryNumber: 'A-12', speciesPolish: 'lipa', speciesLatin: null, trunkCircumferenceCm: 314,
  heightM: 18.5, crownDiameterM: null, notes: null, conditionNotes: 'posusz', hazardNotes: null,
  createdAt: '2026-09-30T10:00:00Z', updatedAt: '2026-09-30T10:00:00Z',
};

test('wartosci formularza pokazuja aktualne dane drzewa (brakujace jako puste pola)', () => {
  const formValues = toTreeFormValues(savedTree);
  assert.equal(formValues.inventoryNumber, 'A-12');
  assert.equal(formValues.heightM, '18,5');
  assert.equal(formValues.crownDiameterM, '');
  assert.equal(formValues.conditionNotes, 'posusz');
  assert.equal(formValues.hazardNotes, '');
});

test('polski przecinek dziesietny trafia do API jako liczba', () => {
  const request = toTreeEditableFieldsRequest({ ...emptyTreeFormValues, inventoryNumber: '1', heightM: '12,5', crownDiameterM: ' 8,25 ' });
  assert.equal(request.heightM, 12.5);
  assert.equal(request.crownDiameterM, 8.25);
  assert.deepEqual(validateTreeFormValues({ ...emptyTreeFormValues, inventoryNumber: '1', heightM: '12,5' }), {});
});

test('walidacja: numer wymagany, bez ujemnych i niepoprawnych pomiarow, limity jak w backendzie', () => {
  const errors = validateTreeFormValues({
    ...emptyTreeFormValues, inventoryNumber: '  ', trunkCircumferenceCm: '-5', heightM: '-1', crownDiameterM: '12,345',
  });
  assert.equal(errors.inventoryNumber, 'Numer drzewa jest wymagany.');
  assert.ok(errors.trunkCircumferenceCm);
  assert.ok(errors.heightM);
  assert.ok(errors.crownDiameterM);
  assert.match(validateTreeFormValues({ ...emptyTreeFormValues, inventoryNumber: '1', heightM: '151' }).heightM, /150/);
});

test('puste pola opisowe sa pomijane w requescie (backend je czysci)', () => {
  const request = toTreeEditableFieldsRequest({ ...emptyTreeFormValues, inventoryNumber: ' 7 ', conditionNotes: '  ', hazardNotes: ' konar ' });
  assert.equal(request.inventoryNumber, '7');
  assert.equal(request.conditionNotes, undefined);
  assert.equal(request.hazardNotes, 'konar');
});

test('"niezapisane zmiany": 18,50 wobec zapisanego 18.5 i dopisane spacje nie sa zmiana', () => {
  const savedValues = toTreeFormValues(savedTree);
  assert.ok(areTreeFormValuesEquivalent({ ...savedValues, heightM: '18,50', conditionNotes: ' posusz ' }, savedValues));
  assert.ok(!areTreeFormValuesEquivalent({ ...savedValues, hazardNotes: 'konar' }, savedValues));
});

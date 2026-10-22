import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { Tree, TreeEditableFieldsRequest } from './treeDataTypes';
import {
  areTreeFormValuesEquivalent, emptyTreeFormValues, toTreeEditableFieldsRequest, toTreeFormValues, toTreeUpdateRequest,
  validateTreeFormValues,
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

// Etap 32 (ochrona danych przed panelem terenowym): zapis wysyla komplet pol, a pominiete pole czysci wartosc
// na backendzie - zmiana jednego pola nie moze zgubic pozostalych.
const fullyFilledTree: Tree = {
  id: 2, projectId: 1, inventoryNumber: '15', speciesPolish: 'dąb szypułkowy', speciesLatin: 'Quercus robur',
  trunkCircumferenceCm: 314, heightM: 18.5, crownDiameterM: 12.25, notes: 'przy ławce', conditionNotes: 'posusz w koronie',
  hazardNotes: 'konar nad chodnikiem', createdAt: '2026-09-30T10:00:00Z', updatedAt: '2026-09-30T10:00:00Z',
};

test('zmiana jednego pola: zadanie zapisu ma pozostale pola z poprzednimi wartosciami', () => {
  const before = toTreeEditableFieldsRequest(toTreeFormValues(fullyFilledTree));
  assert.deepEqual(before, {
    inventoryNumber: '15', speciesLatin: 'Quercus robur', speciesPolish: 'dąb szypułkowy', trunkCircumferenceCm: 314,
    heightM: 18.5, crownDiameterM: 12.25, notes: 'przy ławce', conditionNotes: 'posusz w koronie', hazardNotes: 'konar nad chodnikiem',
  });
  for (const fieldName of Object.keys(emptyTreeFormValues) as (keyof typeof emptyTreeFormValues)[]) {
    const changedValue = fieldName === 'trunkCircumferenceCm' ? '400' : fieldName === 'heightM' || fieldName === 'crownDiameterM' ? '20' : 'zmiana';
    const after = toTreeEditableFieldsRequest({ ...toTreeFormValues(fullyFilledTree), [fieldName]: changedValue });
    for (const otherField of Object.keys(before) as (keyof TreeEditableFieldsRequest)[]) {
      if (otherField !== fieldName) assert.deepEqual(after[otherField], (before as TreeEditableFieldsRequest)[otherField], `${fieldName} -> ${otherField}`);
    }
  }
});

test('wyczyszczenie pola w formularzu = pole pominiete w zadaniu (backend czysci tylko to pole)', () => {
  const request = toTreeEditableFieldsRequest({ ...toTreeFormValues(fullyFilledTree), hazardNotes: '  ' });
  assert.equal('hazardNotes' in request && request.hazardNotes !== undefined, false);
  assert.equal(request.conditionNotes, 'posusz w koronie');
  assert.equal(request.heightM, 18.5);
});

test('etap 37: zapis z wersja danych, ktora widzial formularz; "Zapisz mimo to" i drzewo offline - bez wersji', () => {
  const formValues = toTreeFormValues(fullyFilledTree);
  assert.equal(toTreeUpdateRequest(formValues, { dataVersion: 3 }).expectedDataVersion, 3);
  assert.equal('expectedDataVersion' in toTreeUpdateRequest(formValues, { dataVersion: 3 }, true), false);
  assert.equal('expectedDataVersion' in toTreeUpdateRequest(formValues, {}), false);
  assert.equal(toTreeUpdateRequest(formValues, { dataVersion: 0 }).expectedDataVersion, 0);
});

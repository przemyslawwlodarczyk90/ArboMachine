import assert from 'node:assert/strict';
import { test } from 'node:test';
import { valuationCategoryLabel } from './valuationDataTypes.ts';
import {
  hasUnsavedValuationChanges, isValuationEmpty, toValuationFormValues, toValuationRequest, validateValuationFormValues,
} from './valuationFormValues.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci).

test('brak waloryzacji -> pusty formularz, kategoria "Nie oceniono"', () => {
  const formValues = toValuationFormValues({ treeId: 1, hasValuation: false });
  assert.equal(formValues.overallCategory, '');
  assert.equal(formValues.naturalValue, '');
  assert.equal(isValuationEmpty(formValues), true);
  assert.equal(valuationCategoryLabel(null), 'Nie oceniono');
  assert.equal(valuationCategoryLabel('VERY_HIGH'), 'Bardzo wysoka');
});

test('request: tylko wypelnione pola, tekst przyciety, bez zadnych wyliczen', () => {
  const formValues = { ...toValuationFormValues(null), overallCategory: 'HIGH' as const, naturalValue: '  Okazały egzemplarz  ', notes: '   ' };
  assert.deepEqual(toValuationRequest(formValues), { overallCategory: 'HIGH', naturalValue: 'Okazały egzemplarz' });
  assert.equal(isValuationEmpty(formValues), false);
});

test('niezapisane zmiany wzgledem zapisanej oceny', () => {
  const saved = { treeId: 1, hasValuation: true, overallCategory: 'MEDIUM' as const, landscapeValue: 'Dominanta' };
  const formValues = toValuationFormValues(saved);
  assert.equal(hasUnsavedValuationChanges(formValues, saved), false);
  assert.equal(hasUnsavedValuationChanges({ ...formValues, landscapeValue: 'Dominanta ' }, saved), false, 'spacje na koncu to nie zmiana');
  assert.equal(hasUnsavedValuationChanges({ ...formValues, overallCategory: '' }, saved), true);
});

test('za dlugi tekst - polski komunikat', () => {
  const errors = validateValuationFormValues({ ...toValuationFormValues(null), justification: 'x'.repeat(5001) });
  assert.equal(errors.justification, 'Tekst może mieć maksymalnie 5000 znaków.');
});

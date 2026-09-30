import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  areTreeWorkFormValuesEquivalent, toTreeWorkFieldsRequest, validateTreeWorkFormValues,
} from './treeWorkFormValues.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci).

test('nazwa jest wymagana (same spacje to brak nazwy)', () => {
  assert.equal(validateTreeWorkFormValues({ name: '   ', description: '' }).name, 'Nazwa jest wymagana.');
  assert.deepEqual(validateTreeWorkFormValues({ name: 'Cięcia redukcyjne', description: '' }), {});
});

test('limity dlugosci jak w backendzie: nazwa 255, opis 5000', () => {
  assert.deepEqual(validateTreeWorkFormValues({ name: 'a'.repeat(255), description: 'b'.repeat(5000) }), {});
  const errors = validateTreeWorkFormValues({ name: 'a'.repeat(256), description: 'b'.repeat(5001) });
  assert.match(errors.name, /255/);
  assert.match(errors.description, /5000/);
});

test('request: nazwa przycieta, pusty opis pominiety', () => {
  assert.deepEqual(toTreeWorkFieldsRequest({ name: '  Wycinka  ', description: '   ' }), { name: 'Wycinka', description: undefined });
  assert.deepEqual(toTreeWorkFieldsRequest({ name: 'Wycinka', description: ' Pilne ' }), { name: 'Wycinka', description: 'Pilne' });
});

test('dopisanie samych spacji nie jest zmiana formularza', () => {
  assert.ok(areTreeWorkFormValuesEquivalent({ name: 'Wycinka', description: '' }, { name: ' Wycinka ', description: '  ' }));
  assert.ok(!areTreeWorkFormValuesEquivalent({ name: 'Wycinka', description: '' }, { name: 'Wycinka', description: 'x' }));
});

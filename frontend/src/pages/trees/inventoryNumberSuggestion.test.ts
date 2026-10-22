import assert from 'node:assert/strict';
import { test } from 'node:test';
import { suggestNextInventoryNumber } from './inventoryNumberSuggestion.ts';

// Uruchamianie: npm test (node --test). Proponowany numer drzewa (etap 32, V1).

test('najwiekszy numer czysto liczbowy + 1, niezaleznie od kolejnosci i spacji', () => {
  assert.equal(suggestNextInventoryNumber(['3', '12', ' 7 ']), '13');
  assert.equal(suggestNextInventoryNumber(['12', '3']), '13');
});

test('numery z literami i znakami sa pomijane; brak liczbowych = pusta propozycja', () => {
  assert.equal(suggestNextInventoryNumber(['A-12', '15a', '2.5', '9']), '10');
  assert.equal(suggestNextInventoryNumber(['A-1', 'PARK-15']), '');
  assert.equal(suggestNextInventoryNumber([]), '');
});

test('zera wiodace i bardzo duze numery', () => {
  assert.equal(suggestNextInventoryNumber(['007', '010']), '11');
  assert.equal(suggestNextInventoryNumber(['99999999999999999999']), '100000000000000000000');
});

import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  arePhotoDocumentSettingsEquivalent, toPhotoDocumentSettingsRequest, toPhotoDocumentSettingsValues,
  validatePhotoDocumentSettings,
} from './photoDocumentSettings.ts';

// Uruchamianie: npm test (node --test). Etap 52: podpis zdjecia i wybor do dokumentu PDF.

test('zdjęcie bez ustawień (sprzed etapu 52) jest w dokumencie i nie ma podpisu', () => {
  assert.deepEqual(toPhotoDocumentSettingsValues({}), { caption: '', includedInDocument: true });
  assert.deepEqual(toPhotoDocumentSettingsValues({ caption: 'Posusz', includedInDocument: false }),
    { caption: 'Posusz', includedInDocument: false });
});

test('request: podpis przycięty, pusty pominięty', () => {
  assert.deepEqual(toPhotoDocumentSettingsRequest({ caption: '  Ubytek w pniu ', includedInDocument: true }),
    { caption: 'Ubytek w pniu', includedInDocument: true });
  assert.deepEqual(toPhotoDocumentSettingsRequest({ caption: '   ', includedInDocument: false }),
    { caption: undefined, includedInDocument: false });
});

test('walidacja długości i porównanie bez znaczenia spacji', () => {
  assert.equal(validatePhotoDocumentSettings({ caption: 'x'.repeat(500), includedInDocument: true }), null);
  assert.match(validatePhotoDocumentSettings({ caption: 'x'.repeat(501), includedInDocument: true }) ?? '', /500/);
  const saved = { caption: 'Posusz', includedInDocument: true };
  assert.ok(arePhotoDocumentSettingsEquivalent(saved, { caption: ' Posusz ', includedInDocument: true }));
  assert.ok(!arePhotoDocumentSettingsEquivalent(saved, { caption: 'Posusz', includedInDocument: false }));
});

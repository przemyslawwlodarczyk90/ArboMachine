import assert from 'node:assert/strict';
import { test } from 'node:test';
import { errorOfSelectedPhotoFile, formatFileSizeForDisplay } from './treePhotoFileRules.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci).

test('JPEG, PNG i WebP mozna wyslac', () => {
  for (const type of ['image/jpeg', 'image/png', 'image/webp']) {
    assert.equal(errorOfSelectedPhotoFile({ type, size: 1000 }), null);
  }
});

test('inne typy i pusty plik sa zatrzymywane jeszcze przed wyslaniem', () => {
  assert.match(errorOfSelectedPhotoFile({ type: 'application/pdf', size: 1000 }) ?? '', /Nieobsługiwany format/);
  assert.match(errorOfSelectedPhotoFile({ type: 'image/heic', size: 1000 }) ?? '', /Nieobsługiwany format/);
  assert.match(errorOfSelectedPhotoFile({ type: '', size: 1000 }) ?? '', /Nieobsługiwany format/);
  assert.equal(errorOfSelectedPhotoFile({ type: 'image/jpeg', size: 0 }), 'Wybrany plik jest pusty.');
});

test('rozmiar pliku po polsku', () => {
  assert.equal(formatFileSizeForDisplay(512), '512 B');
  assert.equal(formatFileSizeForDisplay(850 * 1024), '850 KB');
  assert.equal(formatFileSizeForDisplay(3.4 * 1024 * 1024), '3,4 MB');
});

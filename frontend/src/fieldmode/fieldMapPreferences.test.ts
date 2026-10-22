import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { KeyValueStorage } from '../map/basemapPreference.ts';
import {
  FIELD_MAP_VIEW_MODE_STORAGE_KEY, readFieldGnssAveraging, readFieldMapViewMode, saveFieldGnssAveraging, saveFieldMapViewMode,
} from './fieldMapPreferences.ts';

// Uruchamianie: npm test (node --test). Ustawienia mapy trybu terenowego (etap 32).

const memoryStorage = (initial: Record<string, string> = {}): KeyValueStorage => {
  const values = { ...initial };
  return { getItem: (key) => values[key] ?? null, setItem: (key, value) => { values[key] = value; } };
};
const throwingStorage: KeyValueStorage = { getItem: () => { throw new Error('x'); }, setItem: () => { throw new Error('x'); } };

test('tryb mapy w terenie: domyslnie oznaczenia drzew, zapamietany wybor, nieznana wartosc = domyslny', () => {
  assert.equal(readFieldMapViewMode(null), 'MARKINGS');
  assert.equal(readFieldMapViewMode(throwingStorage), 'MARKINGS');
  assert.equal(readFieldMapViewMode(memoryStorage({ [FIELD_MAP_VIEW_MODE_STORAGE_KEY]: 'COS' })), 'MARKINGS');
  const storage = memoryStorage();
  saveFieldMapViewMode(storage, 'VALUATION');
  assert.equal(readFieldMapViewMode(storage), 'VALUATION');
  assert.doesNotThrow(() => saveFieldMapViewMode(throwingStorage, 'SYMBOLS'));
});

test('usrednianie pomiaru: domyslnie wylaczone, zapamietane na urzadzeniu', () => {
  const storage = memoryStorage();
  assert.equal(readFieldGnssAveraging(storage), false);
  saveFieldGnssAveraging(storage, true);
  assert.equal(readFieldGnssAveraging(storage), true);
  assert.equal(readFieldGnssAveraging(throwingStorage), false);
});

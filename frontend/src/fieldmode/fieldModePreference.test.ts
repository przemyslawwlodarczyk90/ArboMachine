import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { KeyValueStorage } from '../map/basemapPreference.ts';
import { readSavedWorkMode, resolveWorkMode, saveWorkMode, WORK_MODE_STORAGE_KEY } from './fieldModePreference.ts';

// Uruchamianie: npm test (node --test). Tryb pracy zapamietany na urzadzeniu (etap 32).

const memoryStorage = (initial: Record<string, string> = {}): KeyValueStorage & { values: Record<string, string> } => {
  const values = { ...initial };
  return { values, getItem: (key) => values[key] ?? null, setItem: (key, value) => { values[key] = value; } };
};

const throwingStorage: KeyValueStorage = {
  getItem: () => { throw new Error('SecurityError'); },
  setItem: () => { throw new Error('QuotaExceededError'); },
};

test('brak magazynu, pusty, uszkodzony albo rzucajacy magazyn = brak zapisanego trybu', () => {
  assert.equal(readSavedWorkMode(null), null);
  assert.equal(readSavedWorkMode(memoryStorage()), null);
  assert.equal(readSavedWorkMode(memoryStorage({ [WORK_MODE_STORAGE_KEY]: 'MOBILE' })), null);
  assert.equal(readSavedWorkMode(throwingStorage), null);
});

test('domyslnie widok biurowy; pierwsze uruchomienie na urzadzeniu dotykowym = tryb terenowy (V1)', () => {
  assert.equal(resolveWorkMode(memoryStorage(), false), 'OFFICE');
  assert.equal(resolveWorkMode(memoryStorage(), true), 'FIELD');
  assert.equal(resolveWorkMode(throwingStorage, false), 'OFFICE');
});

test('zapisany wybor wygrywa z rodzajem wskaznika', () => {
  assert.equal(resolveWorkMode(memoryStorage({ [WORK_MODE_STORAGE_KEY]: 'OFFICE' }), true), 'OFFICE');
  assert.equal(resolveWorkMode(memoryStorage({ [WORK_MODE_STORAGE_KEY]: 'FIELD' }), false), 'FIELD');
});

test('zapis trybu; wyjatek magazynu nie przerywa pracy', () => {
  const storage = memoryStorage();
  saveWorkMode(storage, 'FIELD');
  assert.equal(storage.values[WORK_MODE_STORAGE_KEY], 'FIELD');
  assert.doesNotThrow(() => saveWorkMode(throwingStorage, 'OFFICE'));
  assert.doesNotThrow(() => saveWorkMode(null, 'OFFICE'));
});

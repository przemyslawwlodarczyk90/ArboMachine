import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  BASEMAP_STORAGE_KEY, createBasemapPreferenceStore, DEFAULT_BASEMAP_SELECTION, readBasemapSelection, type KeyValueStorage,
} from './basemapPreference.ts';

const memoryStorage = (initial: Record<string, string> = {}): KeyValueStorage & { values: Record<string, string> } => {
  const values = { ...initial };
  return { values, getItem: (key) => values[key] ?? null, setItem: (key, value) => { values[key] = value; } };
};

test('brak magazynu, pusty albo uszkodzony wpis = podklad domyslny (OSM, bez dzialek)', () => {
  assert.deepEqual(readBasemapSelection(null), DEFAULT_BASEMAP_SELECTION);
  assert.deepEqual(readBasemapSelection(memoryStorage()), DEFAULT_BASEMAP_SELECTION);
  assert.deepEqual(readBasemapSelection(memoryStorage({ [BASEMAP_STORAGE_KEY]: '{zly json' })), DEFAULT_BASEMAP_SELECTION);
});

test('zapamietany niedostepny podklad (Google - zaslepka) wraca do OSM, dzialki zostaja', () => {
  const storage = memoryStorage({ [BASEMAP_STORAGE_KEY]: JSON.stringify({ basemapId: 'GOOGLE', showParcels: true }) });
  assert.deepEqual(readBasemapSelection(storage), { basemapId: 'OSM', showParcels: true });
});

test('zmiana wyboru jest zapisywana i ogloszona subskrybentom; niedostepny podklad ignorowany', () => {
  const storage = memoryStorage();
  const store = createBasemapPreferenceStore(storage);
  let notifications = 0;
  const unsubscribe = store.subscribe(() => { notifications += 1; });
  store.update({ basemapId: 'GEOPORTAL_ORTHOPHOTO' });
  store.update({ showParcels: true });
  store.update({ basemapId: 'GOOGLE' });
  unsubscribe();
  store.update({ basemapId: 'OSM' });
  assert.equal(notifications, 2);
  assert.deepEqual(readBasemapSelection(storage), { basemapId: 'OSM', showParcels: true });
});

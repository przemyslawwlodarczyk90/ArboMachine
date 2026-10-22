import assert from 'node:assert/strict';
import { test } from 'node:test';
import { createUnsavedChangesStore } from './unsavedChangesStore.ts';

// Uruchamianie: npm test (node --test). Rejestr niezapisanych zmian przy przelaczaniu trybu (etap 32).

test('zrodlo z niezapisanymi zmianami jest widoczne do czasu wyczyszczenia', () => {
  const store = createUnsavedChangesStore();
  let notifications = 0;
  store.subscribe(() => { notifications += 1; });
  assert.equal(store.hasUnsavedChanges(), false);
  store.setSource('tree-form', {});
  assert.equal(store.hasUnsavedChanges(), true);
  store.setSource('tree-form', null);
  store.setSource('tree-form', null);
  assert.equal(store.hasUnsavedChanges(), false);
  assert.equal(notifications, 2);
});

test('zapis wszystkich: udany czysci rejestr, nieudany zostawia zmiany', async () => {
  const store = createUnsavedChangesStore();
  store.setSource('a', { save: async () => true });
  store.setSource('b', { save: async () => false });
  assert.equal(store.canSaveAll(), true);
  assert.equal(await store.saveAll(), false);
  assert.equal(store.hasUnsavedChanges(), true);
  store.setSource('b', { save: async () => true });
  assert.equal(await store.saveAll(), true);
  assert.equal(store.hasUnsavedChanges(), false);
});

test('zrodlo bez zapisu: nie da sie zapisac jednym przyciskiem; odrzucenie wywoluje discard', () => {
  const store = createUnsavedChangesStore();
  let discarded = 0;
  store.setSource('a', { discard: () => { discarded += 1; } });
  assert.equal(store.canSaveAll(), false);
  store.discardAll();
  assert.equal(discarded, 1);
  assert.equal(store.hasUnsavedChanges(), false);
});

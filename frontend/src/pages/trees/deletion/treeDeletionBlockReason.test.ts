import { test } from 'node:test';
import assert from 'node:assert/strict';
import { treeDeletionBlockReason } from './treeDeletionBlockReason.ts';

test('drzewo na serwerze, bez zmian w kolejce, z polaczeniem - mozna usunac', () => {
  assert.equal(treeDeletionBlockReason({ isOnline: true, isCreatedOffline: false, hasPendingChanges: false }), null);
});

test('bez polaczenia, z kolejka albo utworzone offline - blokada z powodem', () => {
  assert.match(treeDeletionBlockReason({ isOnline: false, isCreatedOffline: false, hasPendingChanges: false })!, /połączenia/);
  assert.match(treeDeletionBlockReason({ isOnline: true, isCreatedOffline: false, hasPendingChanges: true })!, /czekające na wysłanie/);
  assert.match(treeDeletionBlockReason({ isOnline: false, isCreatedOffline: true, hasPendingChanges: true })!, /nie trafiło jeszcze na serwer/);
});

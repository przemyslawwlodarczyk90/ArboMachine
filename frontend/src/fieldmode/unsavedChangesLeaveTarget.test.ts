import { test } from 'node:test';
import assert from 'node:assert/strict';
import { isLeavingCurrentScreen } from './unsavedChangesLeaveTarget.ts';

const TREE_CARD = { pathname: '/projects/5/trees/12', search: '' };

test('inna sciezka opuszcza ekran', () => {
  assert.equal(isLeavingCurrentScreen(TREE_CARD, { pathname: '/projects/5/trees', search: '' }), true);
  assert.equal(isLeavingCurrentScreen(TREE_CARD, { pathname: '/field/projects/5', search: '?drzewo=12' }), true);
});

test('ta sama sciezka - takze z innym ?drzewo= (pilnuje runGuarded) - nie opuszcza ekranu', () => {
  assert.equal(isLeavingCurrentScreen(TREE_CARD, { pathname: '/projects/5/trees/12', search: '' }), false);
  assert.equal(isLeavingCurrentScreen({ pathname: '/field/projects/5', search: '?drzewo=12' },
    { pathname: '/field/projects/5', search: '?drzewo=13' }), false);
});

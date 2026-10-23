import { test } from 'node:test';
import assert from 'node:assert/strict';
import { internalLeaveTarget, type LinkClick } from './unsavedChangesLeaveTarget.ts';

const CURRENT = 'http://localhost:8083/projects/5/trees/12';
const click = (overrides: Partial<LinkClick>): LinkClick => ({
  href: '/projects/5/trees', target: null, hasDownloadAttribute: false, opensElsewhere: false, ...overrides,
});

test('link do innego ekranu aplikacji opuszcza karte drzewa', () => {
  assert.equal(internalLeaveTarget(click({}), CURRENT), '/projects/5/trees');
  assert.equal(internalLeaveTarget(click({ href: '/field/projects/5?drzewo=12' }), CURRENT), '/field/projects/5?drzewo=12');
});

test('ten sam adres i kotwica nie opuszczaja ekranu', () => {
  assert.equal(internalLeaveTarget(click({ href: '/projects/5/trees/12' }), CURRENT), null);
  assert.equal(internalLeaveTarget(click({ href: '#zdjecia' }), CURRENT), null);
});

test('link zewnetrzny, nowa karta, pobieranie i Ctrl+klik nie sa pilnowane', () => {
  assert.equal(internalLeaveTarget(click({ href: 'https://www.geoportal.gov.pl/' }), CURRENT), null);
  assert.equal(internalLeaveTarget(click({ target: '_blank' }), CURRENT), null);
  assert.equal(internalLeaveTarget(click({ hasDownloadAttribute: true }), CURRENT), null);
  assert.equal(internalLeaveTarget(click({ opensElsewhere: true }), CURRENT), null);
  assert.equal(internalLeaveTarget(click({ href: null }), CURRENT), null);
});

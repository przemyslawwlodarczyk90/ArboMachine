import assert from 'node:assert/strict';
import { test } from 'node:test';
import { parseTreeIdRouteParam } from './parseTreeIdRouteParam.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci).

test('id drzewa z serwera i tymczasowe id drzewa utworzonego offline', () => {
  assert.equal(parseTreeIdRouteParam('15'), 15);
  assert.equal(parseTreeIdRouteParam('-179088095947800'), -179088095947800);
});

test('inne wartosci -> null (strona "nie istnieje")', () => {
  for (const value of [undefined, '', '0', '-0', 'abc', '1.5', '--3', '12a']) assert.equal(parseTreeIdRouteParam(value), null, String(value));
});

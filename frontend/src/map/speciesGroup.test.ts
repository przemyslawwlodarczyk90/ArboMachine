import assert from 'node:assert/strict';
import { test } from 'node:test';
import { speciesGroupOf } from './speciesGroup.ts';

test('grupa gatunku z rodzaju łacińskiego albo polskiej nazwy; brak gatunku = brak grupy', () => {
  assert.equal(speciesGroupOf('Pinus sylvestris', null), 'CONIFER');
  assert.equal(speciesGroupOf('× Cuprocyparis leylandii', null), 'CONIFER');
  assert.equal(speciesGroupOf(null, 'żywotnik zachodni'), 'CONIFER');
  assert.equal(speciesGroupOf(null, 'Świerk pospolity'), 'CONIFER');
  assert.equal(speciesGroupOf('Tilia cordata', 'lipa drobnolistna'), 'DECIDUOUS');
  assert.equal(speciesGroupOf('Ginkgo biloba', null), 'DECIDUOUS', 'miłorząb jak liściaste (V1)');
  assert.equal(speciesGroupOf(null, '  '), null);
  assert.equal(speciesGroupOf(undefined, undefined), null);
});

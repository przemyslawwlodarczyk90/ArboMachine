import assert from 'node:assert/strict';
import { test } from 'node:test';
import { layoutMarkingBadges } from './treeMarkingLayout.ts';

test('jedno oznaczenie - nad punktem, na srodku', () => {
  assert.deepEqual(layoutMarkingBadges(['SNAG']), { badges: [{ code: 'SNAG', displacement: [0, 20] }], overflowCount: 0, overflowDisplacement: [18, 20] });
});

test('dwa oznaczenia - symetrycznie wokol srodka', () => {
  assert.deepEqual(layoutMarkingBadges(['A', 'B']).badges.map((badge) => badge.displacement), [[-9, 20], [9, 20]]);
});

test('ponad 4 oznaczenia - 4 widoczne i "+N" na koncu wysrodkowanego rzedu', () => {
  const layout = layoutMarkingBadges(['A', 'B', 'C', 'D', 'E', 'F']);
  assert.deepEqual(layout.badges.map((badge) => badge.code), ['A', 'B', 'C', 'D']);
  assert.deepEqual(layout.badges.map((badge) => badge.displacement[0]), [-36, -18, 0, 18]);
  assert.equal(layout.overflowCount, 2);
  assert.deepEqual(layout.overflowDisplacement, [36, 20]);
});

test('brak oznaczen - brak znaczkow', () => {
  assert.deepEqual(layoutMarkingBadges([]).badges, []);
  assert.equal(layoutMarkingBadges([]).overflowCount, 0);
});

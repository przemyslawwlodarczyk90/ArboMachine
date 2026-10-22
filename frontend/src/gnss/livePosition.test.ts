import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { GnssReading } from './deviceGnss.ts';
import { chooseLivePositionReading, livePositionStatusAfterError } from './livePosition.ts';

// Uruchamianie: npm test (node --test). Biezaca pozycja w trybie terenowym (etap 32).

const reading = (measuredAt: string, accuracyM: number, latitude = 52.1): GnssReading =>
  ({ latitude, longitude: 21.0, accuracyM, altitudeM: null, measuredAt });

test('pierwszy odczyt zawsze przyjety, nowszy zastepuje poprzedni (takze mniej dokladny)', () => {
  const first = reading('2026-10-22T10:00:00.000Z', 5);
  assert.equal(chooseLivePositionReading(null, first), first);
  const newer = reading('2026-10-22T10:00:01.000Z', 12, 52.2);
  assert.equal(chooseLivePositionReading(first, newer), newer);
});

test('odczyt starszy niz poprzedni jest odrzucany', () => {
  const current = reading('2026-10-22T10:00:05.000Z', 5);
  assert.equal(chooseLivePositionReading(current, reading('2026-10-22T10:00:01.000Z', 1)), current);
});

test('ta sama chwila pomiaru: wygrywa tylko dokladniejszy odczyt', () => {
  const current = reading('2026-10-22T10:00:05.000Z', 5);
  assert.equal(chooseLivePositionReading(current, reading('2026-10-22T10:00:05.000Z', 8)), current);
  const better = reading('2026-10-22T10:00:05.000Z', 2);
  assert.equal(chooseLivePositionReading(current, better), better);
});

test('bledy sledzenia: brak zgody konczy, timeout nie konczy, brak pozycji konczy tylko bez odczytu', () => {
  assert.deepEqual(livePositionStatusAfterError(1, true), { status: 'unavailable', reason: 'PERMISSION_DENIED' });
  assert.deepEqual(livePositionStatusAfterError(3, false), { status: 'watching' });
  assert.deepEqual(livePositionStatusAfterError(2, true), { status: 'watching' });
  assert.deepEqual(livePositionStatusAfterError(2, false), { status: 'unavailable', reason: 'POSITION_UNAVAILABLE' });
});

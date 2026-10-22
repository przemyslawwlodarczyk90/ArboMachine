import assert from 'node:assert/strict';
import { test } from 'node:test';
import { describeFieldLocation, fieldLocationSourceLabel, formatMeasurementTime, formatReportedAccuracy } from './fieldLocationDescription.ts';

// Uruchamianie: npm test (node --test). Opis lokalizacji w trybie terenowym bez slowa "RTK" (etap 32).

test('zrodla punktu po polsku; brak zrodla = punkt sprzed zapisu zrodla', () => {
  assert.equal(fieldLocationSourceLabel('DEVICE_GNSS'), 'GPS urządzenia (przeglądarka)');
  assert.equal(fieldLocationSourceLabel('MAP'), 'Wskazane na mapie');
  assert.equal(fieldLocationSourceLabel('MANUAL'), 'Wpisane ręcznie');
  assert.equal(fieldLocationSourceLabel('IMPORT'), 'Import');
  assert.match(fieldLocationSourceLabel(null), /nieznane/);
});

test('dokladnosc centymetrowa nie jest nazywana RTK - tylko "zgloszona przez urzadzenie"', () => {
  assert.equal(formatReportedAccuracy(0.02), '±2 cm (dokładność zgłoszona przez urządzenie)');
  const description = describeFieldLocation({ source: 'DEVICE_GNSS', accuracyM: 0.02, altitudeM: 104.4 });
  assert.equal(description, 'GPS urządzenia (przeglądarka) · ±2 cm (dokładność zgłoszona przez urządzenie) · wysokość 104 m n.p.m.');
  assert.doesNotMatch(description, /RTK/);
});

test('czas pomiaru jako godzina', () => {
  assert.equal(formatMeasurementTime('2026-11-20T10:31:22Z', 'UTC'), '10:31:22');
});

test('etap 35: zapisany czas pomiaru w opisie punktu', () => {
  assert.equal(describeFieldLocation({ source: 'DEVICE_GNSS', accuracyM: 4, measuredAt: '2026-11-20T10:31:22Z' }, 'UTC'),
    'GPS urządzenia (przeglądarka) · ±4 m (dokładność zgłoszona przez urządzenie) · pomiar o 10:31:22');
});

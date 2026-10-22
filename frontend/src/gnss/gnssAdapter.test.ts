import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  BROWSER_GEOLOCATION_ADAPTER_CODE, BROWSER_GEOLOCATION_AVERAGED_CODE, createBrowserGeolocationAdapter, describeAdapterCode,
  locationFieldsOfFix,
} from './gnssAdapter.ts';

// Uruchamianie: npm test (node --test). Adapter GNSS przegladarki (etap 35).

const position = (latitude: number, accuracy: number, timestamp = Date.parse('2026-10-22T10:31:22Z')) =>
  ({ coords: { latitude, longitude: 21.0, accuracy, altitude: 104.4 }, timestamp });

function fakeGeolocation() {
  return {
    getCurrentPosition: (onSuccess: (value: ReturnType<typeof position>) => void) => onSuccess(position(52.1, 0.03)),
    watchPosition: (onSuccess: (value: ReturnType<typeof position>) => void) => {
      for (let index = 0; index < 10; index++) onSuccess(position(52.1, 0.05, Date.parse('2026-10-22T10:31:22Z') + index * 1000));
      return 1;
    },
    clearWatch: () => {},
  };
}

test('pojedynczy odczyt: kod sposobu pomiaru i czas, bez typu rozwiazania RTK mimo dokladnosci 3 cm', async () => {
  const fix = await createBrowserGeolocationAdapter(fakeGeolocation(), true).measure('SINGLE', () => {}).result;
  assert.equal(fix.adapterCode, BROWSER_GEOLOCATION_ADAPTER_CODE);
  assert.equal(fix.accuracyM, 0.03);
  assert.equal(fix.measuredAt, '2026-10-22T10:31:22.000Z');
  assert.equal(fix.solutionType, undefined);
  assert.equal(fix.deviceLabel, null);
});

test('seria usredniona ma wlasny kod sposobu pomiaru i czas ostatniego odczytu', async () => {
  const fix = await createBrowserGeolocationAdapter(fakeGeolocation(), true).measure('AVERAGED', () => {}).result;
  assert.equal(fix.adapterCode, BROWSER_GEOLOCATION_AVERAGED_CODE);
  assert.equal(fix.measuredAt, '2026-10-22T10:31:31.000Z');
});

test('dostepnosc: brak HTTPS albo brak Geolocation API', async () => {
  assert.equal(createBrowserGeolocationAdapter(fakeGeolocation(), false).isAvailable(), false);
  assert.equal(createBrowserGeolocationAdapter(null, true).isAvailable(), false);
  assert.equal(createBrowserGeolocationAdapter(fakeGeolocation(), true).isAvailable(), true);
  await assert.rejects(createBrowserGeolocationAdapter(fakeGeolocation(), false).measure('SINGLE', () => {}).result,
    (error: Error & { reason?: string }) => error.reason === 'INSECURE_CONTEXT');
});

test('pola lokalizacji z pomiaru: zrodlo GNSS, czas i sposob pomiaru; bez wysokosci i urzadzenia, gdy brak', async () => {
  const fix = await createBrowserGeolocationAdapter(fakeGeolocation(), true).measure('SINGLE', () => {}).result;
  assert.deepEqual(locationFieldsOfFix(fix), {
    latitude: 52.1, longitude: 21, accuracyM: 0.03, altitudeM: 104.4, source: 'DEVICE_GNSS',
    measuredAt: '2026-10-22T10:31:22.000Z', adapterCode: 'browser-geolocation',
  });
  assert.equal('altitudeM' in locationFieldsOfFix({ ...fix, altitudeM: null }), false);
});

test('opis sposobu pomiaru', () => {
  assert.match(describeAdapterCode('browser-geolocation-averaged')!, /średnia/);
  assert.equal(describeAdapterCode('vendor-ble'), 'vendor-ble');
  assert.equal(describeAdapterCode(null), null);
});

import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  describeLocationMeasurement, formatAccuracy, GNSS_READING_OPTIONS, gnssAccuracyQuality, GnssReadingError,
  readDeviceGnssPosition, type GeolocationLike,
} from './deviceGnss.ts';

// Uruchamianie: npm test (node --test). navigator.geolocation podstawiony - bez przegladarki i bez GPS.

const position: { coords: { latitude: number; longitude: number; accuracy: number; altitude: number | null }; timestamp: number } =
  { coords: { latitude: 52.22967561234, longitude: 21.0122287999, accuracy: 4.276, altitude: 104.4567 }, timestamp: 1_700_000_000_000 };

const geolocationReturning = (result: { position?: typeof position; errorCode?: number }) => {
  const calls: unknown[] = [];
  const geolocation: GeolocationLike = {
    getCurrentPosition: (onSuccess, onError, options) => {
      calls.push(options);
      if (result.position) onSuccess(result.position);
      else onError({ code: result.errorCode ?? 2 });
    },
  };
  return { geolocation, calls };
};

test('pomiar: wspolrzedne zaokraglone do 7 miejsc, dokladnosc i wysokosc do 2, opcje wysokiej dokladnosci', async () => {
  const { geolocation, calls } = geolocationReturning({ position });
  const reading = await readDeviceGnssPosition(geolocation, true);
  assert.deepEqual(reading, {
    latitude: 52.2296756, longitude: 21.0122288, accuracyM: 4.28, altitudeM: 104.46, measuredAt: '2023-11-14T22:13:20.000Z',
  });
  assert.deepEqual(calls, [GNSS_READING_OPTIONS]);
  assert.equal(GNSS_READING_OPTIONS.enableHighAccuracy, true);
});

test('brak wysokosci z urzadzenia -> altitudeM null', async () => {
  const { geolocation } = geolocationReturning({ position: { ...position, coords: { ...position.coords, altitude: null } } });
  assert.equal((await readDeviceGnssPosition(geolocation, true)).altitudeM, null);
});

test('bledy Geolocation API -> polskie komunikaty', async () => {
  const cases: [number, string, RegExp][] = [
    [1, 'PERMISSION_DENIED', /brak zgody/],
    [2, 'POSITION_UNAVAILABLE', /nie ustaliło pozycji/],
    [3, 'TIMEOUT', /30 sekund/],
  ];
  for (const [errorCode, reason, message] of cases) {
    const { geolocation } = geolocationReturning({ errorCode });
    await assert.rejects(readDeviceGnssPosition(geolocation, true), (error: unknown) => {
      assert.ok(error instanceof GnssReadingError);
      assert.equal(error.reason, reason);
      assert.match(error.message, /^Nie uzyskano lokalizacji/);
      assert.match(error.message, message);
      return true;
    });
  }
});

test('brak Geolocation API albo strona bez HTTPS - bez pytania o zgode', async () => {
  await assert.rejects(readDeviceGnssPosition(null, true), (error: unknown) =>
    error instanceof GnssReadingError && error.reason === 'UNSUPPORTED');
  const { geolocation, calls } = geolocationReturning({ position });
  await assert.rejects(readDeviceGnssPosition(geolocation, false), (error: unknown) =>
    error instanceof GnssReadingError && error.reason === 'INSECURE_CONTEXT' && /HTTPS/.test(error.message));
  assert.equal(calls.length, 0);
});

test('ocena dokladnosci V1: do 5 m dobra, do 15 m akceptowalna, powyzej slaba', () => {
  assert.equal(gnssAccuracyQuality(3), 'GOOD');
  assert.equal(gnssAccuracyQuality(5), 'GOOD');
  assert.equal(gnssAccuracyQuality(12.5), 'ACCEPTABLE');
  assert.equal(gnssAccuracyQuality(65), 'POOR');
});

test('opis zapisanego pomiaru po polsku', () => {
  assert.equal(formatAccuracy(4.28), '±4,3 m');
  assert.equal(describeLocationMeasurement({ source: 'DEVICE_GNSS', accuracyM: 4.28, altitudeM: 104.46 }),
    'GPS urządzenia, dokładność ±4,3 m, wysokość 104 m n.p.m.');
  assert.equal(describeLocationMeasurement({ source: 'MAP' }), 'Wskazane na mapie');
  assert.equal(describeLocationMeasurement({}), null);
  assert.equal(describeLocationMeasurement({ source: null, accuracyM: null }), null);
});

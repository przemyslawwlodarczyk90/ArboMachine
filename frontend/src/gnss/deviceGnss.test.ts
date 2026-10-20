import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  describeLocationMeasurement, formatAccuracy, GNSS_READING_OPTIONS, gnssAccuracyQuality, GnssReadingError,
  readDeviceGnssPosition, type GeolocationLike, averageGnssReadings, collectAveragedGnssReading, type GeolocationWatchLike,
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

test('progi dokladnosci RTK: do 10 cm geodezyjna, do 1 m submetrowa; ponizej metra w centymetrach', () => {
  assert.equal(gnssAccuracyQuality(0.02), 'SURVEY');
  assert.equal(gnssAccuracyQuality(0.1), 'SURVEY');
  assert.equal(gnssAccuracyQuality(0.6), 'SUBMETER');
  assert.equal(formatAccuracy(0.02), '±2 cm');
  assert.equal(formatAccuracy(1), '±1 m');
});

const reading = (latitude: number, longitude: number, accuracyM: number, altitudeM: number | null = null) =>
  ({ latitude, longitude, accuracyM, altitudeM, measuredAt: '2026-10-20T10:00:00.000Z' });

test('usrednianie: srednia wspolrzednych, rozrzut w metrach, dokladnosc nie lepsza niz rozrzut', () => {
  const averaged = averageGnssReadings([reading(52.0, 21.0, 0.02, 100), reading(52.00001, 21.0, 0.02, 102)]);
  assert.equal(averaged.latitude, 52.000005);
  assert.equal(averaged.sampleCount, 2);
  assert.equal(averaged.spreadM, 0.56);
  assert.equal(averaged.accuracyM, 0.56);
  assert.equal(averaged.altitudeM, 101);
  assert.equal(averageGnssReadings([reading(52, 21, 3), reading(52, 21, 5)]).accuracyM, 4);
});

test('seria odczytow konczy sie po 10 odczytach; po czasie usrednia zebrane; bez odczytow = TIMEOUT', async () => {
  let cleared = 0;
  let emit: (latitude: number) => void = () => {};
  const geolocation: GeolocationWatchLike = {
    watchPosition: (onSuccess) => {
      emit = (latitude) => onSuccess({ coords: { latitude, longitude: 21, accuracy: 0.03, altitude: null }, timestamp: 0 });
      return 7;
    },
    clearWatch: (watchId) => { if (watchId === 7) cleared += 1; },
  };
  const progress: number[] = [];
  const full = collectAveragedGnssReading(geolocation, true, (count) => progress.push(count), () => undefined);
  for (let index = 0; index < 12; index++) emit(52);
  assert.equal((await full.result).sampleCount, 10);
  assert.equal(progress.length, 10);
  assert.equal(cleared, 1);

  let fireTimeout = () => {};
  const timed = collectAveragedGnssReading(geolocation, true, () => {}, (callback) => { fireTimeout = callback; });
  emit(52);
  emit(52.00001);
  fireTimeout();
  assert.equal((await timed.result).sampleCount, 2);

  const empty = collectAveragedGnssReading(geolocation, true, () => {}, (callback) => { fireTimeout = callback; });
  fireTimeout();
  await assert.rejects(empty.result, (error: GnssReadingError) => error.reason === 'TIMEOUT');
});

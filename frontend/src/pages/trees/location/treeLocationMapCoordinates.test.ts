import assert from 'node:assert/strict';
import { describe, test } from 'node:test';
import { fromLonLat } from 'ol/proj.js';
import { toMapCoordinate, toTreeLocation } from './treeLocationMapCoordinates.ts';
import { isTreeLocationWithinValidRange } from './treeLocationValidRange.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci). Test pilnuje przede wszystkim kolejnosci
// wspolrzednych: OpenLayers [longitude, latitude] vs API { latitude, longitude }.

const WARSAW = { latitude: 52.2296756, longitude: 21.0122287 };

describe('toMapCoordinate', () => {
  test('podaje OpenLayers [longitude, latitude], a nie odwrotnie', () => {
    assert.deepEqual(toMapCoordinate(WARSAW), fromLonLat([WARSAW.longitude, WARSAW.latitude]));
    assert.notDeepEqual(toMapCoordinate(WARSAW), fromLonLat([WARSAW.latitude, WARSAW.longitude]));
  });

  test('zwraca wspolrzedne EPSG:3857 w metrach, a nie stopnie', () => {
    // Wzor Web Mercator liczony niezaleznie od OpenLayers: x = R*lon, y = R*ln(tan(pi/4 + lat/2)).
    const earthRadiusMeters = 6_378_137;
    const toRadians = (degrees: number) => (degrees * Math.PI) / 180;
    const expectedX = earthRadiusMeters * toRadians(WARSAW.longitude);
    const expectedY = earthRadiusMeters * Math.log(Math.tan(Math.PI / 4 + toRadians(WARSAW.latitude) / 2));
    const [x, y] = toMapCoordinate(WARSAW);
    assert.ok(Math.abs(x - expectedX) < 0.01, `x=${x}, oczekiwano ${expectedX}`);
    assert.ok(Math.abs(y - expectedY) < 0.01, `y=${y}, oczekiwano ${expectedY}`);
  });
});

describe('toTreeLocation', () => {
  test('zamienia punkt mapy na { latitude, longitude } w WGS84', () => {
    assert.deepEqual(toTreeLocation(toMapCoordinate(WARSAW)), WARSAW);
  });

  test('szerokosc i dlugosc nie sa zamienione (punkt na polkuli poludniowo-zachodniej)', () => {
    const buenosAires = { latitude: -34.6037, longitude: -58.3816 };
    const convertedLocation = toTreeLocation(fromLonLat([buenosAires.longitude, buenosAires.latitude]));
    assert.equal(convertedLocation.latitude, buenosAires.latitude);
    assert.equal(convertedLocation.longitude, buenosAires.longitude);
  });

  test('zaokragla do 7 miejsc po przecinku (skala kolumn backendu)', () => {
    const convertedLocation = toTreeLocation(fromLonLat([21.012228749, 52.229675649]));
    assert.deepEqual(convertedLocation, { latitude: 52.2296756, longitude: 21.0122287 });
  });

  test('sprowadza dlugosc do zakresu -180..180 po przewinieciu mapy dookola swiata', () => {
    const [x, y] = fromLonLat([21, 52]);
    const fullWorldWidthInMeters = 2 * 20_037_508.342789244;
    const convertedLocation = toTreeLocation([x + fullWorldWidthInMeters, y]);
    assert.ok(Math.abs(convertedLocation.longitude - 21) < 1e-6, `longitude=${convertedLocation.longitude}`);
    assert.ok(isTreeLocationWithinValidRange(convertedLocation));
  });
});

describe('isTreeLocationWithinValidRange', () => {
  test('akceptuje granice zakresu WGS84 i punkty poza Polska', () => {
    assert.ok(isTreeLocationWithinValidRange({ latitude: -90, longitude: -180 }));
    assert.ok(isTreeLocationWithinValidRange({ latitude: 90, longitude: 180 }));
    assert.ok(isTreeLocationWithinValidRange({ latitude: 40.7128, longitude: -74.006 }));
  });

  test('odrzuca wartosci poza zakresem i NaN', () => {
    assert.equal(isTreeLocationWithinValidRange({ latitude: 90.0000001, longitude: 0 }), false);
    assert.equal(isTreeLocationWithinValidRange({ latitude: 0, longitude: -180.5 }), false);
    assert.equal(isTreeLocationWithinValidRange({ latitude: Number.NaN, longitude: 0 }), false);
  });
});

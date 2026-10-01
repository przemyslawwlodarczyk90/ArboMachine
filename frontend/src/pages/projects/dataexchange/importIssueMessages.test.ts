import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { TreeImportIssue } from './dataExchangeDataTypes.ts';
import { describeImportIssue, locationOfIssue } from './importIssueMessages.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci).

const issue = (overrides: Partial<TreeImportIssue>): TreeImportIssue =>
  ({ rowNumber: null, field: null, column: null, value: null, code: 'EMPTY_FILE', ...overrides });

test('bledy wierszy po polsku z numerem wiersza, kolumna z pliku i wartoscia', () => {
  assert.equal(describeImportIssue(issue({ rowNumber: 17, code: 'REQUIRED_VALUE_MISSING', column: 'Numer inwentaryzacyjny' }), 'CSV'),
    'Wiersz 17: brak numeru inwentaryzacyjnego.');
  assert.equal(describeImportIssue(issue({ rowNumber: 42, code: 'INVALID_NUMBER', column: 'Wysokość [m]', value: 'dużo' }), 'XLSX'),
    'Wiersz 42: nieprawidłowa liczba w kolumnie „Wysokość [m]” („dużo”).');
  assert.equal(describeImportIssue(issue({ rowNumber: 91, code: 'VALUE_OUT_OF_RANGE', column: 'Szerokość geograficzna', value: '95' }), 'CSV'),
    'Wiersz 91: wartość spoza dopuszczalnego zakresu w kolumnie „Szerokość geograficzna” („95”).');
});

test('GeoJSON: "Obiekt N" zamiast "Wiersz N"', () => {
  const geometryIssue = issue({ rowNumber: 3, code: 'UNSUPPORTED_GEOMETRY', value: 'Polygon' });
  assert.equal(locationOfIssue(geometryIssue, 'GEOJSON'), 'Obiekt 3');
  assert.match(describeImportIssue(geometryIssue, 'GEOJSON'), /^Obiekt 3: nieobsługiwana geometria \(„Polygon”\)/);
});

test('bledy calego pliku bez numeru wiersza', () => {
  assert.equal(locationOfIssue(issue({ code: 'EMPTY_FILE' }), 'CSV'), null);
  assert.equal(describeImportIssue(issue({ code: 'EMPTY_FILE' }), 'CSV'), 'Plik jest pusty.');
  assert.equal(describeImportIssue(issue({ code: 'MISSING_REQUIRED_COLUMN', column: 'Numer inwentaryzacyjny' }), 'CSV'),
    'Brak wymaganej kolumny „Numer inwentaryzacyjny”.');
  assert.match(describeImportIssue(issue({ code: 'INVALID_ENCODING' }), 'CSV'), /UTF-8/);
  assert.match(describeImportIssue(issue({ code: 'UNSUPPORTED_CRS', column: 'urn:ogc:def:crs:EPSG::2180' }), 'GEOJSON'), /EPSG::2180.*EPSG:4326/);
});

test('ostrzezenia o duplikatach mowia, ze powstanie nowe drzewo', () => {
  assert.match(describeImportIssue(issue({ rowNumber: 2, code: 'INVENTORY_NUMBER_EXISTS_IN_PROJECT', value: 'L-1' }), 'CSV'),
    /Wiersz 2: drzewo o numerze „L-1” już jest w projekcie - import utworzy nowe drzewo/);
  assert.match(describeImportIssue(issue({ rowNumber: 4, code: 'DUPLICATE_INVENTORY_NUMBER_IN_FILE', value: 'D-2' }), 'CSV'),
    /powtarza się w pliku/);
});

test('zaden kod nie konczy sie technicznym komunikatem', () => {
  const codes = ['UNSUPPORTED_FILE_FORMAT', 'FILE_TOO_LARGE', 'EMPTY_FILE', 'UNREADABLE_FILE', 'INVALID_ENCODING', 'INVALID_GEOJSON',
    'UNSUPPORTED_CRS', 'MISSING_REQUIRED_COLUMN', 'NO_DATA_ROWS', 'TOO_MANY_ROWS', 'REQUIRED_VALUE_MISSING', 'TEXT_TOO_LONG',
    'INVALID_NUMBER', 'NOT_A_WHOLE_NUMBER', 'TOO_MANY_DECIMAL_PLACES', 'VALUE_OUT_OF_RANGE', 'INCOMPLETE_LOCATION',
    'UNSUPPORTED_GEOMETRY', 'DUPLICATE_INVENTORY_NUMBER_IN_FILE', 'INVENTORY_NUMBER_EXISTS_IN_PROJECT'] as const;
  for (const code of codes) {
    const message = describeImportIssue(issue({ rowNumber: code.startsWith('UNSUPPORTED_F') ? null : 5, code, column: 'Kolumna', value: 'x' }), 'CSV');
    assert.ok(message.length > 10 && !/Exception|null|undefined/.test(message), `${code}: ${message}`);
  }
});

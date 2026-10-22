import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  canCompressPhoto, compressedPhotoFileName, isCompressedPhotoWorthUsing, readCompressPhotosPreference,
  saveCompressPhotosPreference, scaledPhotoDimensions,
} from './photoCompression.ts';

// Uruchamianie: npm test (node --test). Etap 40: zmniejszanie zdjec przed wyslaniem.

const MB = 1024 * 1024;

test('zmniejszamy tylko JPEG i WebP powyzej 1 MB', () => {
  assert.equal(canCompressPhoto({ type: 'image/jpeg', size: 4 * MB }), true);
  assert.equal(canCompressPhoto({ type: 'image/webp', size: 2 * MB }), true);
  assert.equal(canCompressPhoto({ type: 'image/png', size: 8 * MB }), false);
  assert.equal(canCompressPhoto({ type: 'image/jpeg', size: MB }), false);
});

test('wymiary: dluzszy bok do 2560 px z zachowaniem proporcji, tez w pionie', () => {
  assert.deepEqual(scaledPhotoDimensions(4000, 3000), { width: 2560, height: 1920 });
  assert.deepEqual(scaledPhotoDimensions(3000, 4000), { width: 1920, height: 2560 });
  assert.deepEqual(scaledPhotoDimensions(2000, 1500), { width: 2000, height: 1500 });
});

test('nazwa po zmianie formatu', () => {
  assert.equal(compressedPhotoFileName('IMG_1.webp'), 'IMG_1.jpg');
  assert.equal(compressedPhotoFileName('drzewo.12.jpeg'), 'drzewo.12.jpg');
  assert.equal(compressedPhotoFileName('bez_rozszerzenia'), 'bez_rozszerzenia.jpg');
  assert.equal(compressedPhotoFileName('.jpg'), '.jpg.jpg');
});

test('zmniejszona wersja tylko przy co najmniej 10% oszczednosci', () => {
  assert.equal(isCompressedPhotoWorthUsing(4 * MB, MB), true);
  assert.equal(isCompressedPhotoWorthUsing(4 * MB, 3.8 * MB), false);
  assert.equal(isCompressedPhotoWorthUsing(4 * MB, 0), false);
});

test('wybor zapamietany na urzadzeniu, domyslnie wylaczony', () => {
  const values = new Map<string, string>();
  const storage = { getItem: (key: string) => values.get(key) ?? null, setItem: (key: string, value: string) => { values.set(key, value); } };
  assert.equal(readCompressPhotosPreference(storage), false);
  saveCompressPhotosPreference(storage, true);
  assert.equal(readCompressPhotosPreference(storage), true);
  const blocked = { getItem: () => { throw new Error('zablokowane'); }, setItem: () => { throw new Error('zablokowane'); } };
  assert.equal(readCompressPhotosPreference(blocked), false);
  saveCompressPhotosPreference(blocked, true);
});

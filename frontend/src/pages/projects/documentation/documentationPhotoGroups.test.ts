import assert from 'node:assert/strict';
import { test } from 'node:test';
import type { ProjectPhoto } from '../../trees/photos/treePhotoDataTypes.ts';
import { groupProjectPhotosByTree } from './documentationPhotoGroups.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci).

const photo = (photoId: number, treeId: number, inventoryNumber: string, hasAnnotation = false): ProjectPhoto => ({
  photoId, treeId, inventoryNumber, originalFileName: `IMG_${photoId}.jpg`, contentType: 'image/jpeg', fileSizeBytes: 1000,
  hasAnnotation, annotationUpdatedAt: hasAnnotation ? '2026-10-01T10:00:00Z' : null, createdAt: '2026-10-01T09:00:00Z',
});

test('zdjecia grupowane po drzewie z zachowaniem kolejnosci z backendu', () => {
  const groups = groupProjectPhotosByTree([photo(1, 10, '7'), photo(2, 10, '7', true), photo(3, 20, '8'), photo(4, 10, '7')]);
  assert.deepEqual(groups.map((group) => [group.treeId, group.inventoryNumber, group.photos.map((p) => p.photoId)]),
    [[10, '7', [1, 2, 4]], [20, '8', [3]]]);
  assert.deepEqual(groups.map((group) => group.annotatedPhotoCount), [1, 0]);
});

test('brak zdjec -> brak grup', () => {
  assert.deepEqual(groupProjectPhotosByTree([]), []);
});
